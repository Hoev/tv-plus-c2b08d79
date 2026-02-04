import React, { useState, useEffect } from 'react';
import { getTokenCount, NotificationActionType } from '@/lib/fcm';
import { ref, get } from 'firebase/database';
import { db } from '@/lib/firebase';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Bell, Send, Users, AlertCircle, CheckCircle, AlertTriangle } from 'lucide-react';
import type { Category, SideMenu, Channel, SubChannel } from '@/types/admin';

const NotificationManager: React.FC = () => {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [actionType, setActionType] = useState<NotificationActionType>('main_channel');
  const [selectedMainChannel, setSelectedMainChannel] = useState('');
  const [selectedSideMenu, setSelectedSideMenu] = useState('');
  const [selectedSubChannel, setSelectedSubChannel] = useState('');
  const [externalUrl, setExternalUrl] = useState('');
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<{ success: boolean; message: string } | null>(null);
  const [tokenCount, setTokenCount] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  // Data from Firebase
  const [categories, setCategories] = useState<Record<string, Category>>({});
  const [sideMenus, setSideMenus] = useState<Record<string, SideMenu>>({});

  // Fetch token count and data
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // Fetch token count
        const count = await getTokenCount();
        setTokenCount(count);

        // Fetch categories
        const categoriesSnapshot = await get(ref(db, 'categories'));
        if (categoriesSnapshot.exists()) {
          setCategories(categoriesSnapshot.val());
        }

        // Fetch side menus
        const sideMenusSnapshot = await get(ref(db, 'sideMenus'));
        if (sideMenusSnapshot.exists()) {
          setSideMenus(sideMenusSnapshot.val());
        }
      } catch (error) {
        console.error('Error fetching data:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  // Get all main channels from categories
  const getAllMainChannels = (): { id: string; name: string; categoryName: string }[] => {
    const channels: { id: string; name: string; categoryName: string }[] = [];
    Object.values(categories).forEach(category => {
      if (category.channels) {
        Object.values(category.channels).forEach(channel => {
          channels.push({
            id: channel.id,
            name: channel.name,
            categoryName: category.name
          });
        });
      }
    });
    return channels;
  };

  // Get sub-channels from selected side menu
  const getSubChannels = (): SubChannel[] => {
    if (!selectedSideMenu || !sideMenus[selectedSideMenu]) return [];
    const menu = sideMenus[selectedSideMenu];
    return menu.channels ? Object.values(menu.channels) : [];
  };

  const refreshTokenCount = async () => {
    const count = await getTokenCount();
    setTokenCount(count);
  };

  const buildPayload = () => {
    const payload: any = {
      actionType,
    };

    switch (actionType) {
      case 'main_channel':
        payload.targetId = selectedMainChannel;
        break;
      case 'side_menu':
        payload.targetId = selectedSideMenu;
        break;
      case 'sub_channel':
        payload.targetId = selectedSubChannel;
        payload.parentMenuId = selectedSideMenu;
        break;
      case 'external_link':
        payload.externalUrl = externalUrl;
        break;
    }

    return payload;
  };

  const validateForm = (): boolean => {
    if (!title.trim() || !body.trim()) return false;
    
    switch (actionType) {
      case 'main_channel':
        return !!selectedMainChannel;
      case 'side_menu':
        return !!selectedSideMenu;
      case 'sub_channel':
        return !!selectedSideMenu && !!selectedSubChannel;
      case 'external_link':
        return !!externalUrl.trim();
      default:
        return false;
    }
  };

  const sendNotification = async () => {
    if (!validateForm()) {
      setResult({ success: false, message: 'الرجاء ملء جميع الحقول المطلوبة' });
      return;
    }

    if (tokenCount === 0) {
      setResult({ success: false, message: 'لا توجد أجهزة مسجلة للإشعارات' });
      return;
    }

    setSending(true);
    setResult(null);

    try {
      const payload = buildPayload();
      
      // Note: Sending FCM messages requires a server-side implementation
      // This prepares the payload that would be sent via Firebase Cloud Functions
      const notificationData = {
        notification: {
          title: title.trim(),
          body: body.trim(),
        },
        data: payload,
        tokenCount: tokenCount
      };

      console.log('Notification payload prepared:', notificationData);

      // For now, show success with instructions
      setResult({ 
        success: true, 
        message: `تم تجهيز الإشعار لـ ${tokenCount} جهاز. ملاحظة: يتطلب الإرسال الفعلي Firebase Cloud Functions أو خادم خلفي.`
      });

      // Clear form
      setTitle('');
      setBody('');
      setExternalUrl('');
      setSelectedMainChannel('');
      setSelectedSideMenu('');
      setSelectedSubChannel('');
    } catch (error) {
      console.error('Error preparing notification:', error);
      setResult({ success: false, message: 'حدث خطأ أثناء تجهيز الإشعار' });
    } finally {
      setSending(false);
    }
  };

  const isFormDisabled = tokenCount === 0;
  const mainChannels = getAllMainChannels();
  const subChannels = getSubChannels();

  if (loading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Token Count Card */}
      <Card className="border-border bg-card">
        <CardContent className="pt-6">
          <div className="flex items-center gap-3 p-4 rounded-lg bg-secondary">
            <Users className="w-6 h-6 text-primary" />
            <div className="flex-1">
              <p className="text-sm text-muted-foreground">الأجهزة المسجلة للإشعارات</p>
              <p className="text-2xl font-bold text-foreground">{tokenCount ?? 0}</p>
            </div>
            <Button variant="outline" size="sm" onClick={refreshTokenCount}>
              تحديث
            </Button>
          </div>
          
          {tokenCount === 0 && (
            <div className="flex items-center gap-2 mt-4 p-3 rounded-lg bg-yellow-900/30 text-yellow-400">
              <AlertTriangle className="w-5 h-5" />
              <span className="text-sm">لا يوجد مستخدمين مشتركين في الإشعارات حتى الآن</span>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Send Notification Form */}
      <Card className={`border-border bg-card ${isFormDisabled ? 'opacity-60' : ''}`}>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Bell className="w-5 h-5 text-primary" />
            إرسال إشعار جديد
          </CardTitle>
          <CardDescription>
            أرسل إشعارات فورية لجميع المستخدمين المسجلين مع إمكانية التوجيه المباشر
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Title */}
          <div className="space-y-2">
            <Label>عنوان الإشعار <span className="text-destructive">*</span></Label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="مثال: مباراة جديدة الآن!"
              className="bg-secondary border-border"
              disabled={isFormDisabled}
            />
          </div>

          {/* Body */}
          <div className="space-y-2">
            <Label>محتوى الإشعار <span className="text-destructive">*</span></Label>
            <Textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="مثال: شاهد مباراة ريال مدريد ضد برشلونة الآن"
              className="bg-secondary border-border min-h-[80px]"
              disabled={isFormDisabled}
            />
          </div>

          {/* Action Type */}
          <div className="space-y-2">
            <Label>نوع الإجراء عند النقر <span className="text-destructive">*</span></Label>
            <Select
              value={actionType}
              onValueChange={(value: NotificationActionType) => {
                setActionType(value);
                setSelectedMainChannel('');
                setSelectedSideMenu('');
                setSelectedSubChannel('');
                setExternalUrl('');
              }}
              disabled={isFormDisabled}
            >
              <SelectTrigger className="bg-secondary border-border">
                <SelectValue placeholder="اختر نوع الإجراء" />
              </SelectTrigger>
              <SelectContent className="bg-card border-border">
                <SelectItem value="main_channel">فتح قناة رئيسية</SelectItem>
                <SelectItem value="side_menu">فتح قائمة جانبية</SelectItem>
                <SelectItem value="sub_channel">فتح قناة فرعية</SelectItem>
                <SelectItem value="external_link">رابط خارجي</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Dynamic Fields based on Action Type */}
          {actionType === 'main_channel' && (
            <div className="space-y-2">
              <Label>اختر القناة <span className="text-destructive">*</span></Label>
              <Select
                value={selectedMainChannel}
                onValueChange={setSelectedMainChannel}
                disabled={isFormDisabled}
              >
                <SelectTrigger className="bg-secondary border-border">
                  <SelectValue placeholder="اختر قناة" />
                </SelectTrigger>
                <SelectContent className="bg-card border-border max-h-[200px]">
                  {mainChannels.map(channel => (
                    <SelectItem key={channel.id} value={channel.id}>
                      {channel.name} ({channel.categoryName})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {actionType === 'side_menu' && (
            <div className="space-y-2">
              <Label>اختر القائمة الجانبية <span className="text-destructive">*</span></Label>
              <Select
                value={selectedSideMenu}
                onValueChange={setSelectedSideMenu}
                disabled={isFormDisabled}
              >
                <SelectTrigger className="bg-secondary border-border">
                  <SelectValue placeholder="اختر قائمة" />
                </SelectTrigger>
                <SelectContent className="bg-card border-border">
                  {Object.values(sideMenus).map(menu => (
                    <SelectItem key={menu.id} value={menu.id}>
                      {menu.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          {actionType === 'sub_channel' && (
            <>
              <div className="space-y-2">
                <Label>اختر القائمة الجانبية <span className="text-destructive">*</span></Label>
                <Select
                  value={selectedSideMenu}
                  onValueChange={(value) => {
                    setSelectedSideMenu(value);
                    setSelectedSubChannel('');
                  }}
                  disabled={isFormDisabled}
                >
                  <SelectTrigger className="bg-secondary border-border">
                    <SelectValue placeholder="اختر قائمة" />
                  </SelectTrigger>
                  <SelectContent className="bg-card border-border">
                    {Object.values(sideMenus).map(menu => (
                      <SelectItem key={menu.id} value={menu.id}>
                        {menu.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {selectedSideMenu && (
                <div className="space-y-2">
                  <Label>اختر القناة الفرعية <span className="text-destructive">*</span></Label>
                  <Select
                    value={selectedSubChannel}
                    onValueChange={setSelectedSubChannel}
                    disabled={isFormDisabled}
                  >
                    <SelectTrigger className="bg-secondary border-border">
                      <SelectValue placeholder="اختر قناة فرعية" />
                    </SelectTrigger>
                    <SelectContent className="bg-card border-border">
                      {subChannels.map(channel => (
                        <SelectItem key={channel.id} value={channel.id}>
                          {channel.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}
            </>
          )}

          {actionType === 'external_link' && (
            <div className="space-y-2">
              <Label>الرابط الخارجي <span className="text-destructive">*</span></Label>
              <Input
                value={externalUrl}
                onChange={(e) => setExternalUrl(e.target.value)}
                placeholder="https://example.com"
                className="bg-secondary border-border font-mono"
                dir="ltr"
                disabled={isFormDisabled}
              />
              <p className="text-xs text-muted-foreground">
                سيتم فتح الرابط في نافذة جديدة
              </p>
            </div>
          )}

          {/* Result Message */}
          {result && (
            <div className={`flex items-center gap-2 p-3 rounded-lg ${
              result.success ? 'bg-green-500/10 text-green-500' : 'bg-destructive/10 text-destructive'
            }`}>
              {result.success ? (
                <CheckCircle className="w-5 h-5" />
              ) : (
                <AlertCircle className="w-5 h-5" />
              )}
              <span className="text-sm">{result.message}</span>
            </div>
          )}

          {/* Send Button */}
          <Button 
            onClick={sendNotification}
            disabled={sending || isFormDisabled || !validateForm()}
            className="w-full bg-primary text-primary-foreground"
          >
            {sending ? (
              <>
                <div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin ml-2" />
                جاري الإرسال...
              </>
            ) : (
              <>
                <Send className="w-4 h-4 ml-2" />
                إرسال الإشعار
              </>
            )}
          </Button>
        </CardContent>
      </Card>

      {/* Instructions */}
      <Card className="border-border bg-card">
        <CardHeader>
          <CardTitle className="text-base">ملاحظات مهمة</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm text-muted-foreground">
          <p>• يتم طلب إذن الإشعارات تلقائياً عند زيارة المستخدم للتطبيق لأول مرة</p>
          <p>• الإشعارات تعمل حتى عندما يكون التطبيق مغلقاً</p>
          <p>• يمكنك توجيه المستخدم إلى قناة أو قائمة أو رابط خارجي</p>
          <p>• لإرسال الإشعارات فعلياً، تحتاج إلى Firebase Cloud Functions</p>
        </CardContent>
      </Card>
    </div>
  );
};

export default NotificationManager;
