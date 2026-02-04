import React, { useState } from 'react';
import { getAllTokens } from '@/lib/fcm';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Bell, Send, Users, AlertCircle, CheckCircle } from 'lucide-react';

const NotificationManager: React.FC = () => {
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [targetRoute, setTargetRoute] = useState('');
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState<{ success: boolean; message: string } | null>(null);
  const [tokenCount, setTokenCount] = useState<number | null>(null);

  const fetchTokenCount = async () => {
    const tokens = await getAllTokens();
    setTokenCount(tokens.length);
  };

  React.useEffect(() => {
    fetchTokenCount();
  }, []);

  const sendNotification = async () => {
    if (!title.trim() || !body.trim()) {
      setResult({ success: false, message: 'الرجاء إدخال العنوان والمحتوى' });
      return;
    }

    setSending(true);
    setResult(null);

    try {
      const tokens = await getAllTokens();
      
      if (tokens.length === 0) {
        setResult({ success: false, message: 'لا توجد أجهزة مسجلة للإشعارات' });
        setSending(false);
        return;
      }

      // Note: Sending FCM messages requires a server-side implementation
      // This is a client-side simulation that shows how it would work
      // In production, you would call a Firebase Cloud Function or backend API
      
      const payload = {
        notification: {
          title: title.trim(),
          body: body.trim(),
        },
        data: {
          route: targetRoute.trim() || '/',
        },
        tokens: tokens
      };

      console.log('Notification payload prepared:', payload);
      console.log('Would send to', tokens.length, 'devices');

      // For now, show success with instructions
      setResult({ 
        success: true, 
        message: `تم تجهيز الإشعار لـ ${tokens.length} جهاز. ملاحظة: يتطلب إرسال الإشعارات الفعلي خادم Firebase Cloud Functions.`
      });

      // Clear form
      setTitle('');
      setBody('');
      setTargetRoute('');
    } catch (error) {
      console.error('Error preparing notification:', error);
      setResult({ success: false, message: 'حدث خطأ أثناء تجهيز الإشعار' });
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card className="border-border bg-card">
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Bell className="w-5 h-5 text-primary" />
            إرسال إشعار جديد
          </CardTitle>
          <CardDescription>
            أرسل إشعارات فورية لجميع المستخدمين المسجلين
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Token Count */}
          <div className="flex items-center gap-2 p-3 rounded-lg bg-secondary">
            <Users className="w-5 h-5 text-primary" />
            <span className="text-sm text-muted-foreground">
              الأجهزة المسجلة: <strong className="text-foreground">{tokenCount ?? '...'}</strong>
            </span>
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={fetchTokenCount}
              className="mr-auto"
            >
              تحديث
            </Button>
          </div>

          {/* Title */}
          <div className="space-y-2">
            <Label>عنوان الإشعار <span className="text-destructive">*</span></Label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="مثال: مباراة جديدة الآن!"
              className="bg-secondary border-border"
            />
          </div>

          {/* Body */}
          <div className="space-y-2">
            <Label>محتوى الإشعار <span className="text-destructive">*</span></Label>
            <Textarea
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="مثال: شاهد مباراة ريال مدريد ضد برشلونة الآن على beIN Sports"
              className="bg-secondary border-border min-h-[100px]"
            />
          </div>

          {/* Target Route (Deep Link) */}
          <div className="space-y-2">
            <Label>رابط الوجهة (اختياري)</Label>
            <Input
              value={targetRoute}
              onChange={(e) => setTargetRoute(e.target.value)}
              placeholder="مثال: /channel/123 أو /"
              className="bg-secondary border-border font-mono"
              dir="ltr"
            />
            <p className="text-xs text-muted-foreground">
              عند النقر على الإشعار، سيتم توجيه المستخدم إلى هذه الصفحة
            </p>
          </div>

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
            disabled={sending || !title.trim() || !body.trim()}
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
          <p>• يجب على المستخدمين تفعيل الإشعارات من صفحة الإعدادات أولاً</p>
          <p>• الإشعارات تعمل حتى عندما يكون التطبيق مغلقاً</p>
          <p>• رابط الوجهة يدعم Deep Linking للتوجيه المباشر</p>
          <p>• لإرسال إشعارات فعلية، تحتاج إلى Firebase Cloud Functions</p>
        </CardContent>
      </Card>
    </div>
  );
};

export default NotificationManager;
