import React, { useState, useEffect } from 'react';
import { ref, onValue, push, update, remove, set } from 'firebase/database';
import { db } from '@/lib/firebase';
import { Channel, Category, SideMenu, StreamConfig, ActionType } from '@/types/admin';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose } from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { ScrollArea } from '@/components/ui/scroll-area';
import PlayerConfigForm from './PlayerConfigForm';
import ImageUploader from './ImageUploader';
import { Plus, Edit2, Trash2, Play, Menu, Tv, ChevronUp, ChevronDown, ExternalLink } from 'lucide-react';

interface ChannelManagerProps {
  category: Category;
}

const ChannelManager: React.FC<ChannelManagerProps> = ({ category }) => {
  const [channels, setChannels] = useState<Record<string, Channel>>({});
  const [sideMenus, setSideMenus] = useState<Record<string, SideMenu>>({});
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingChannel, setEditingChannel] = useState<Channel | null>(null);

  const sectionId = category?.id;
  
  const [formData, setFormData] = useState<Partial<Channel>>({
    name: '',
    imageUrl: '',
    sortOrder: 0,
    actionType: 'direct_play',
    stream: { url: '' },
    sideMenuId: '',
    externalUrl: ''
  });

  useEffect(() => {
    if (!sectionId) return;

    const channelsRef = ref(db, `categories/${sectionId}/channels`);
    const unsubscribeChannels = onValue(
      channelsRef,
      (snapshot) => {
        setChannels(snapshot.val() || {});
      },
      (err) => {
        console.error('Firebase channels read error:', err);
        alert('تعذر تحميل القنوات من Firebase. تأكد من صلاحيات قاعدة البيانات (Rules).');
      }
    );

    const sideMenusRef = ref(db, 'sideMenus');
    const unsubscribeSideMenus = onValue(
      sideMenusRef,
      (snapshot) => {
        setSideMenus(snapshot.val() || {});
      },
      (err) => {
        console.error('Firebase sideMenus read error:', err);
        alert('تعذر تحميل القوائم الجانبية من Firebase. تأكد من صلاحيات قاعدة البيانات (Rules).');
      }
    );

    return () => {
      unsubscribeChannels();
      unsubscribeSideMenus();
    };
  }, [sectionId]);

  const resetForm = () => {
    setFormData({
      name: '',
      imageUrl: '',
      sortOrder: Object.keys(channels).length,
      actionType: 'direct_play',
      stream: { url: '' },
      sideMenuId: '',
      externalUrl: ''
    });
    setEditingChannel(null);
  };

  const openAddDialog = () => {
    resetForm();
    setFormData(prev => ({ ...prev, sortOrder: Object.keys(channels).length }));
    setIsDialogOpen(true);
  };

  const openEditDialog = (channel: Channel) => {
    setEditingChannel(channel);
    setFormData({
      name: channel.name,
      imageUrl: channel.imageUrl,
      sortOrder: channel.sortOrder,
      actionType: channel.actionType,
      stream: channel.stream || { url: '' },
      sideMenuId: channel.sideMenuId || '',
      externalUrl: channel.externalUrl || ''
    });
    setIsDialogOpen(true);
  };

  const handleSave = async () => {
    if (!sectionId) {
      alert('Please select a valid section');
      return;
    }

    if (!formData.name?.trim()) {
      alert('الرجاء إدخال اسم القناة.');
      return;
    }

    // Validate sideMenuId when action type is open_submenu
    if (formData.actionType === 'open_submenu') {
      if (!formData.sideMenuId || formData.sideMenuId.trim() === '') {
        alert('الرجاء اختيار قائمة جانبية صالحة.');
        return;
      }
      // Verify the selected side menu exists
      if (!sideMenus[formData.sideMenuId]) {
        alert('القائمة الجانبية المحددة غير موجودة. الرجاء اختيار قائمة أخرى.');
        return;
      }
    }

    // Validate external URL when action type is external_link
    if (formData.actionType === 'external_link') {
      if (!formData.externalUrl || formData.externalUrl.trim() === '') {
        alert('الرجاء إدخال رابط الموقع الخارجي.');
        return;
      }
    }
    
    // Build channel data - Firebase doesn't accept undefined values, so we use null or omit them
    const channelData: Record<string, any> = {
      name: formData.name.trim(),
      imageUrl: formData.imageUrl?.trim() || '',
      sortOrder: formData.sortOrder ?? 0,
      actionType: formData.actionType || 'direct_play'
    };

    if (formData.actionType === 'direct_play') {
      // For direct play, include stream config and explicitly set others to null
      channelData.stream = formData.stream || { url: '' };
      channelData.sideMenuId = null;
      channelData.externalUrl = null;
    } else if (formData.actionType === 'open_submenu') {
      // For open_submenu, include sideMenuId and set others to null
      channelData.sideMenuId = formData.sideMenuId;
      channelData.stream = null;
      channelData.externalUrl = null;
    } else if (formData.actionType === 'external_link') {
      // For external_link, include externalUrl and set others to null
      channelData.externalUrl = formData.externalUrl?.trim();
      channelData.stream = null;
      channelData.sideMenuId = null;
    }

    // Debug logging
    console.log('=== Channel Save Debug ===');
    console.log('sectionId:', sectionId);
    console.log('editingChannel:', editingChannel);
    console.log('channelData:', JSON.stringify(channelData, null, 2));
    console.log('Firebase path:', `categories/${sectionId}/channels`);

    try {
      if (editingChannel) {
        const updatePath = `categories/${sectionId}/channels/${editingChannel.id}`;
        console.log('Updating at path:', updatePath);
        await update(ref(db, updatePath), channelData);
      } else {
        const newRef = push(ref(db, `categories/${sectionId}/channels`));
        console.log('Creating new channel with key:', newRef.key);
        await set(newRef, { ...channelData, id: newRef.key });
      }

      console.log('Save successful!');
      setIsDialogOpen(false);
      resetForm();
    } catch (err: any) {
      console.error('Firebase save channel error:', err);
      console.error('Error code:', err?.code);
      console.error('Error message:', err?.message);
      alert(`فشل حفظ القناة: ${err?.message || 'خطأ غير معروف'}`);
    }
  };

  const handleDelete = async (channelId: string) => {
    if (!confirm('هل تريد حذف هذه القناة؟')) return;
    if (!sectionId) {
      alert('Please select a valid section');
      return;
    }
    try {
      await remove(ref(db, `categories/${sectionId}/channels/${channelId}`));
    } catch (err) {
      console.error('Firebase delete channel error:', err);
      alert('فشل حذف القناة. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  const handleMove = async (channelId: string, direction: 'up' | 'down') => {
    const sortedChannels = Object.values(channels).sort((a, b) => a.sortOrder - b.sortOrder);
    const currentIndex = sortedChannels.findIndex(c => c.id === channelId);

    if (!sectionId) {
      alert('Please select a valid section');
      return;
    }
    
    try {
      if (direction === 'up' && currentIndex > 0) {
        const prevChannel = sortedChannels[currentIndex - 1];
        const currentChannel = sortedChannels[currentIndex];
        
        await update(ref(db, `categories/${sectionId}/channels/${channelId}`), { sortOrder: prevChannel.sortOrder });
        await update(ref(db, `categories/${sectionId}/channels/${prevChannel.id}`), { sortOrder: currentChannel.sortOrder });
      } else if (direction === 'down' && currentIndex < sortedChannels.length - 1) {
        const nextChannel = sortedChannels[currentIndex + 1];
        const currentChannel = sortedChannels[currentIndex];
        
        await update(ref(db, `categories/${sectionId}/channels/${channelId}`), { sortOrder: nextChannel.sortOrder });
        await update(ref(db, `categories/${sectionId}/channels/${nextChannel.id}`), { sortOrder: currentChannel.sortOrder });
      }
    } catch (err) {
      console.error('Firebase move channel error:', err);
      alert('فشل تغيير ترتيب القنوات. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  const sortedChannels = Object.values(channels).sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <Card className="border-border bg-card">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg font-bold text-foreground flex items-center gap-2">
          <Tv className="w-5 h-5 text-primary" />
          القنوات داخل "{category.name}"
        </CardTitle>
        <Button size="sm" onClick={openAddDialog} className="bg-primary text-primary-foreground">
          <Plus className="w-4 h-4 mr-2" />
          إضافة قناة
        </Button>
      </CardHeader>
      <CardContent>
        {sortedChannels.length === 0 ? (
          <p className="text-muted-foreground text-sm text-center py-8">لا توجد قنوات داخل هذا القسم</p>
        ) : (
          <div className="space-y-2">
            {sortedChannels.map((channel, index) => (
              <div
                key={channel.id}
                className="flex items-center gap-3 p-3 rounded-lg bg-secondary hover:bg-secondary/80 transition-colors"
              >
                <div className="flex flex-col gap-0.5">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-5 w-5 p-0"
                    onClick={() => handleMove(channel.id, 'up')}
                    disabled={index === 0}
                  >
                    <ChevronUp className="w-4 h-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-5 w-5 p-0"
                    onClick={() => handleMove(channel.id, 'down')}
                    disabled={index === sortedChannels.length - 1}
                  >
                    <ChevronDown className="w-4 h-4" />
                  </Button>
                </div>
                
                <div className="w-12 h-12 rounded-lg bg-background flex items-center justify-center overflow-hidden">
                  {channel.imageUrl ? (
                    <img 
                      src={channel.imageUrl} 
                      alt={channel.name}
                      className="w-10 h-10 object-contain"
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = 'https://via.placeholder.com/40?text=TV';
                      }}
                    />
                  ) : (
                    <Tv className="w-6 h-6 text-muted-foreground" />
                  )}
                </div>
                
                <div className="flex-1">
                  <p className="font-medium text-foreground">{channel.name}</p>
                  <div className="flex items-center gap-2 text-xs text-muted-foreground">
                    {channel.actionType === 'direct_play' ? (
                      <>
                        <Play className="w-3 h-3" />
                        <span>تشغيل مباشر</span>
                      </>
                    ) : channel.actionType === 'external_link' ? (
                      <>
                        <ExternalLink className="w-3 h-3" />
                        <span className="truncate max-w-[200px]">رابط: {channel.externalUrl}</span>
                      </>
                    ) : (
                      <>
                        <Menu className="w-3 h-3" />
                        <span>قائمة فرعية: {sideMenus[channel.sideMenuId || '']?.name || 'غير محدد'}</span>
                      </>
                    )}
                  </div>
                </div>
                
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => openEditDialog(channel)}
                >
                  <Edit2 className="w-4 h-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="text-destructive hover:text-destructive"
                  onClick={() => handleDelete(channel.id)}
                >
                  <Trash2 className="w-4 h-4" />
                </Button>
              </div>
            ))}
          </div>
        )}
      </CardContent>

      {/* Add/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="bg-card border-border max-w-2xl max-h-[90vh]">
          <DialogHeader>
            <DialogTitle>{editingChannel ? 'تعديل القناة' : 'إضافة قناة جديدة'}</DialogTitle>
          </DialogHeader>
          <ScrollArea className="max-h-[60vh] pr-4">
            <div className="space-y-6 py-4">
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label>اسم القناة <span className="text-destructive">*</span></Label>
                  <Input
                    value={formData.name || ''}
                    onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                    placeholder="مثال: beIN Sports 1"
                    className="bg-secondary border-border"
                  />
                </div>
                <ImageUploader
                  value={formData.imageUrl || ''}
                  onChange={(base64) => setFormData(prev => ({ ...prev, imageUrl: base64 }))}
                  label="صورة القناة"
                />
              </div>

              <div className="space-y-3">
                <Label>نوع الإجراء</Label>
                <RadioGroup
                  value={formData.actionType}
                  onValueChange={(value: ActionType) => setFormData(prev => ({ ...prev, actionType: value }))}
                  className="flex flex-wrap gap-4"
                >
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="direct_play" id="direct_play" />
                    <Label htmlFor="direct_play" className="flex items-center gap-2 cursor-pointer">
                      <Play className="w-4 h-4 text-primary" />
                      تشغيل مباشر
                    </Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="open_submenu" id="open_submenu" />
                    <Label htmlFor="open_submenu" className="flex items-center gap-2 cursor-pointer">
                      <Menu className="w-4 h-4 text-primary" />
                      فتح قائمة فرعية
                    </Label>
                  </div>
                  <div className="flex items-center gap-2">
                    <RadioGroupItem value="external_link" id="external_link" />
                    <Label htmlFor="external_link" className="flex items-center gap-2 cursor-pointer">
                      <ExternalLink className="w-4 h-4 text-primary" />
                      رابط خارجي
                    </Label>
                  </div>
                </RadioGroup>
              </div>

              {formData.actionType === 'direct_play' && (
                <PlayerConfigForm
                  streamConfig={formData.stream || { url: '' }}
                  onChange={(stream) => setFormData(prev => ({ ...prev, stream }))}
                />
              )}

              {formData.actionType === 'open_submenu' && (
                <div className="space-y-2">
                  <Label>اختر قائمة جانبية <span className="text-destructive">*</span></Label>
                  <Select
                    value={formData.sideMenuId || undefined}
                    onValueChange={(value) => {
                      console.log('Selected sideMenuId:', value);
                      if (value) {
                        setFormData(prev => ({ ...prev, sideMenuId: value }));
                      }
                    }}
                  >
                    <SelectTrigger className="bg-secondary border-border">
                      <SelectValue placeholder="اختر قائمة جانبية" />
                    </SelectTrigger>
                    <SelectContent className="bg-popover border-border z-50">
                      {Object.entries(sideMenus).map(([id, menu]) => (
                        <SelectItem key={id} value={id}>
                          {menu.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {!formData.sideMenuId && (
                    <p className="text-xs text-amber-500">
                      يجب اختيار قائمة جانبية قبل الحفظ.
                    </p>
                  )}
                  {Object.keys(sideMenus).length === 0 && (
                    <p className="text-xs text-muted-foreground">
                      لا توجد قوائم جانبية بعد. أنشئ واحدة من تبويب "القوائم الجانبية".
                    </p>
                  )}
                </div>
              )}

              {formData.actionType === 'external_link' && (
                <div className="space-y-2">
                  <Label>رابط الموقع <span className="text-destructive">*</span></Label>
                  <Input
                    value={formData.externalUrl || ''}
                    onChange={(e) => setFormData(prev => ({ ...prev, externalUrl: e.target.value }))}
                    placeholder="https://example.com"
                    className="bg-secondary border-border font-mono text-sm"
                  />
                  <p className="text-xs text-muted-foreground">
                    عند النقر على القناة، سيتم توجيه المستخدم إلى هذا الرابط مباشرة.
                  </p>
                </div>
              )}
            </div>
          </ScrollArea>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">إلغاء</Button>
            </DialogClose>
            <Button onClick={handleSave} className="bg-primary text-primary-foreground">
              {editingChannel ? 'حفظ التغييرات' : 'إضافة القناة'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  );
};

export default ChannelManager;
