import React, { useState, useEffect } from 'react';
import { ref, onValue, push, update, remove, set } from 'firebase/database';
import { db } from '@/lib/firebase';
import { SideMenu, SubChannel, StreamConfig, AndroidStreamConfig, AndroidActionType } from '@/types/admin';
import type { WebPlayerType, iOSPlayerApp } from '@/types/admin';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose } from '@/components/ui/dialog';
import { ScrollArea } from '@/components/ui/scroll-area';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import ImageUploader from './ImageUploader';
import WebConfigForm from './WebConfigForm';
import AndroidConfigForm from './AndroidConfigForm';
import { Plus, Edit2, Trash2, Menu, Tv, ChevronUp, ChevronDown, Globe, Smartphone, Eye, EyeOff } from 'lucide-react';

const SideMenuManager: React.FC = () => {
  const [sideMenus, setSideMenus] = useState<Record<string, SideMenu>>({});
  const [isMenuDialogOpen, setIsMenuDialogOpen] = useState(false);
  const [isChannelDialogOpen, setIsChannelDialogOpen] = useState(false);
  const [editingMenu, setEditingMenu] = useState<SideMenu | null>(null);
  const [editingChannel, setEditingChannel] = useState<{ menuId: string; channel: SubChannel | null }>({ menuId: '', channel: null });
  
  const [menuName, setMenuName] = useState('');
  
  // Sub-channel form state with split Web/Android config
  const [channelForm, setChannelForm] = useState<{
    name: string;
    imageUrl: string;
    sortOrder: number;
    // Web settings
    stream: StreamConfig;
    preferredPlayer: WebPlayerType;
    // Android settings
    androidStream: Partial<AndroidStreamConfig>;
    androidActionType: AndroidActionType;
  }>({
    name: '',
    imageUrl: '',
    sortOrder: 0,
    stream: { url: '' },
    preferredPlayer: 'default',
    androidStream: {},
    androidActionType: 'native'
  });

  useEffect(() => {
    const sideMenusRef = ref(db, 'sideMenus');
    const unsubscribe = onValue(
      sideMenusRef,
      (snapshot) => {
        setSideMenus(snapshot.val() || {});
      },
      (err) => {
        console.error('Firebase sideMenus read error:', err);
        alert('تعذر تحميل القوائم الجانبية من Firebase. تأكد من صلاحيات قاعدة البيانات (Rules).');
      }
    );
    return () => unsubscribe();
  }, []);

  // Menu handlers
  const openAddMenuDialog = () => {
    setEditingMenu(null);
    setMenuName('');
    setIsMenuDialogOpen(true);
  };

  const openEditMenuDialog = (menu: SideMenu) => {
    setEditingMenu(menu);
    setMenuName(menu.name);
    setIsMenuDialogOpen(true);
  };

  const handleSaveMenu = async () => {
    if (!menuName.trim()) return;
    
    try {
      if (editingMenu) {
        await update(ref(db, `sideMenus/${editingMenu.id}`), { name: menuName.trim() });
      } else {
        const newRef = push(ref(db, 'sideMenus'));
        await set(newRef, {
          id: newRef.key,
          name: menuName.trim(),
          channels: {}
        });
      }
      
      setIsMenuDialogOpen(false);
      setMenuName('');
      setEditingMenu(null);
    } catch (err) {
      console.error('Firebase save sideMenu error:', err);
      alert('فشل حفظ القائمة الجانبية. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  const handleDeleteMenu = async (menuId: string) => {
    if (!confirm('هل تريد حذف هذه القائمة الجانبية وجميع القنوات بداخلها؟')) return;
    try {
      await remove(ref(db, `sideMenus/${menuId}`));
    } catch (err) {
      console.error('Firebase delete sideMenu error:', err);
      alert('فشل حذف القائمة الجانبية. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  // Channel handlers
  const openAddChannelDialog = (menuId: string) => {
    const menu = sideMenus[menuId];
    const channelCount = Object.keys(menu?.channels || {}).length;
    
    setEditingChannel({ menuId, channel: null });
    setChannelForm({
      name: '',
      imageUrl: '',
      sortOrder: channelCount,
      stream: { url: '' },
      preferredPlayer: 'default',
      androidStream: {},
      androidActionType: 'native'
    });
    setIsChannelDialogOpen(true);
  };

  const openEditChannelDialog = (menuId: string, channel: SubChannel) => {
    setEditingChannel({ menuId, channel });
    setChannelForm({
      name: channel.name,
      imageUrl: channel.imageUrl,
      sortOrder: channel.sortOrder,
      stream: channel.stream || { url: '' },
      preferredPlayer: channel.preferredPlayer || 'default',
      androidStream: channel.androidStream || {},
      androidActionType: channel.androidActionType || 'native'
    });
    setIsChannelDialogOpen(true);
  };

  const handleSaveChannel = async () => {
    if (!channelForm.name?.trim() || !editingChannel.menuId) return;
    
    const channelData: Partial<SubChannel> = {
      name: channelForm.name.trim(),
      imageUrl: channelForm.imageUrl?.trim() || '',
      sortOrder: channelForm.sortOrder || 0,
      // Web settings
      stream: channelForm.stream,
      preferredPlayer: channelForm.preferredPlayer || 'default',
      // Android settings
      androidStream: channelForm.androidStream as AndroidStreamConfig,
      androidActionType: channelForm.androidActionType || 'native'
    };

    try {
      if (editingChannel.channel) {
        await update(
          ref(db, `sideMenus/${editingChannel.menuId}/channels/${editingChannel.channel.id}`),
          channelData
        );
      } else {
        const newRef = push(ref(db, `sideMenus/${editingChannel.menuId}/channels`));
        await set(newRef, { ...channelData, id: newRef.key });
      }

      setIsChannelDialogOpen(false);
      setChannelForm({
        name: '',
        imageUrl: '',
        sortOrder: 0,
        stream: { url: '' },
        preferredPlayer: 'default',
        androidStream: {},
        androidActionType: 'native'
      });
    } catch (err) {
      console.error('Firebase save sub-channel error:', err);
      alert('فشل حفظ القناة داخل القائمة الجانبية. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  const handleDeleteChannel = async (menuId: string, channelId: string) => {
    if (!confirm('هل تريد حذف هذه القناة؟')) return;
    try {
      await remove(ref(db, `sideMenus/${menuId}/channels/${channelId}`));
    } catch (err) {
      console.error('Firebase delete sub-channel error:', err);
      alert('فشل حذف القناة من القائمة الجانبية. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  const handleMoveChannel = async (menuId: string, channelId: string, direction: 'up' | 'down') => {
    const menu = sideMenus[menuId];
    if (!menu?.channels) return;
    
    const sortedChannels = Object.values(menu.channels).sort((a, b) => a.sortOrder - b.sortOrder);
    const currentIndex = sortedChannels.findIndex(c => c.id === channelId);
    
    try {
      if (direction === 'up' && currentIndex > 0) {
        const prevChannel = sortedChannels[currentIndex - 1];
        const currentChannel = sortedChannels[currentIndex];
        
        await update(ref(db, `sideMenus/${menuId}/channels/${channelId}`), { sortOrder: prevChannel.sortOrder });
        await update(ref(db, `sideMenus/${menuId}/channels/${prevChannel.id}`), { sortOrder: currentChannel.sortOrder });
      } else if (direction === 'down' && currentIndex < sortedChannels.length - 1) {
        const nextChannel = sortedChannels[currentIndex + 1];
        const currentChannel = sortedChannels[currentIndex];
        
        await update(ref(db, `sideMenus/${menuId}/channels/${channelId}`), { sortOrder: nextChannel.sortOrder });
        await update(ref(db, `sideMenus/${menuId}/channels/${nextChannel.id}`), { sortOrder: currentChannel.sortOrder });
      }
    } catch (err) {
      console.error('Firebase move sub-channel error:', err);
      alert('فشل تغيير ترتيب القنوات داخل القائمة الجانبية. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  return (
    <Card className="border-border bg-card">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg font-bold text-foreground flex items-center gap-2">
          <Menu className="w-5 h-5 text-primary" />
          القوائم الجانبية (مجموعات فرعية)
        </CardTitle>
        <Button size="sm" onClick={openAddMenuDialog} className="bg-primary text-primary-foreground">
          <Plus className="w-4 h-4 mr-2" />
          إضافة قائمة
        </Button>
      </CardHeader>
      <CardContent>
        {Object.keys(sideMenus).length === 0 ? (
          <p className="text-muted-foreground text-sm text-center py-8">لا توجد قوائم جانبية بعد</p>
        ) : (
          <Accordion type="multiple" className="space-y-2">
            {Object.values(sideMenus).map((menu) => {
              const sortedChannels = Object.values(menu.channels || {}).sort((a, b) => a.sortOrder - b.sortOrder);
              
              return (
                <AccordionItem key={menu.id} value={menu.id} className="border border-border rounded-lg overflow-hidden">
                  <AccordionTrigger className="px-4 py-3 bg-secondary hover:bg-secondary/80 [&[data-state=open]]:bg-primary/10">
                    <div className="flex items-center gap-3 flex-1">
                      <Menu className="w-5 h-5 text-primary" />
                      <span className="font-medium text-foreground">{menu.name}</span>
                      <span className="text-xs text-muted-foreground">
                        {sortedChannels.length} قناة
                      </span>
                    </div>
                    <div className="flex items-center gap-2 mr-2">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8"
                        onClick={(e) => {
                          e.stopPropagation();
                          openEditMenuDialog(menu);
                        }}
                      >
                        <Edit2 className="w-4 h-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 text-destructive hover:text-destructive"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteMenu(menu.id);
                        }}
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="px-4 py-3 bg-card">
                    <div className="space-y-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => openAddChannelDialog(menu.id)}
                        className="w-full mb-3"
                      >
                        <Plus className="w-4 h-4 mr-2" />
                        إضافة قناة للقائمة
                      </Button>
                      
                      {sortedChannels.length === 0 ? (
                        <p className="text-muted-foreground text-sm text-center py-4">لا توجد قنوات داخل هذه القائمة</p>
                      ) : (
                        sortedChannels.map((channel, index) => (
                          <div
                            key={channel.id}
                            className="flex items-center gap-3 p-3 rounded-lg bg-secondary"
                          >
                            <div className="flex flex-col gap-0.5">
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-5 w-5 p-0"
                                onClick={() => handleMoveChannel(menu.id, channel.id, 'up')}
                                disabled={index === 0}
                              >
                                <ChevronUp className="w-4 h-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                className="h-5 w-5 p-0"
                                onClick={() => handleMoveChannel(menu.id, channel.id, 'down')}
                                disabled={index === sortedChannels.length - 1}
                              >
                                <ChevronDown className="w-4 h-4" />
                              </Button>
                            </div>
                            
                            <div className="w-10 h-10 rounded-lg bg-background flex items-center justify-center overflow-hidden">
                              {channel.imageUrl ? (
                                <img 
                                  src={channel.imageUrl} 
                                  alt={channel.name}
                                  className="w-8 h-8 object-contain"
                                  onError={(e) => {
                                    (e.target as HTMLImageElement).src = 'https://via.placeholder.com/32?text=TV';
                                  }}
                                />
                              ) : (
                                <Tv className="w-5 h-5 text-muted-foreground" />
                              )}
                            </div>
                            
                            <div className="flex-1">
                              <span className={`font-medium block ${channel.hidden ? 'text-muted-foreground line-through' : 'text-foreground'}`}>
                                {channel.name}
                                {channel.hidden && <span className="text-xs text-amber-500 mr-2"> (مخفي)</span>}
                              </span>
                              <div className="flex items-center gap-2 mt-1">
                                {channel.stream?.url && (
                                  <span className="text-xs bg-blue-500/20 text-blue-400 px-1.5 py-0.5 rounded">
                                    <Globe className="w-3 h-3 inline mr-1" />
                                    Web
                                  </span>
                                )}
                                {channel.androidStream?.url && (
                                  <span className="text-xs bg-green-500/20 text-green-400 px-1.5 py-0.5 rounded">
                                    <Smartphone className="w-3 h-3 inline mr-1" />
                                    Android
                                  </span>
                                )}
                              </div>
                            </div>
                            
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              title={channel.hidden ? 'إظهار القناة' : 'إخفاء القناة'}
                              onClick={async () => {
                                try {
                                  await update(ref(db, `sideMenus/${menu.id}/channels/${channel.id}`), { hidden: !channel.hidden });
                                } catch (err) {
                                  console.error('Toggle hidden error:', err);
                                }
                              }}
                            >
                              {channel.hidden ? <EyeOff className="w-4 h-4 text-amber-500" /> : <Eye className="w-4 h-4" />}
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={() => openEditChannelDialog(menu.id, channel)}
                            >
                              <Edit2 className="w-4 h-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-destructive hover:text-destructive"
                              onClick={() => handleDeleteChannel(menu.id, channel.id)}
                            >
                              <Trash2 className="w-4 h-4" />
                            </Button>
                          </div>
                        ))
                      )}
                    </div>
                  </AccordionContent>
                </AccordionItem>
              );
            })}
          </Accordion>
        )}
      </CardContent>

      {/* Menu Dialog */}
      <Dialog open={isMenuDialogOpen} onOpenChange={setIsMenuDialogOpen}>
        <DialogContent className="bg-card border-border">
          <DialogHeader>
            <DialogTitle>{editingMenu ? 'تعديل القائمة الجانبية' : 'إضافة قائمة جانبية'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>اسم القائمة</Label>
              <Input
                value={menuName}
                onChange={(e) => setMenuName(e.target.value)}
                placeholder="مثال: باقة رياضة، قنوات عربية"
                className="bg-secondary border-border"
              />
            </div>
          </div>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">إلغاء</Button>
            </DialogClose>
            <Button onClick={handleSaveMenu} className="bg-primary text-primary-foreground">
              {editingMenu ? 'حفظ التغييرات' : 'إضافة القائمة'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Sub-Channel Dialog with Split Web/Android Config */}
      <Dialog open={isChannelDialogOpen} onOpenChange={setIsChannelDialogOpen}>
        <DialogContent className="bg-card border-border max-w-3xl max-h-[90vh]">
          <DialogHeader>
            <DialogTitle>
              {editingChannel.channel ? 'تعديل قناة فرعية' : 'إضافة قناة فرعية'}
            </DialogTitle>
          </DialogHeader>
          <ScrollArea className="max-h-[70vh] pr-4">
            <div className="space-y-6 py-4">
              {/* Basic Info */}
              <div className="space-y-4 p-4 rounded-lg border border-border bg-muted/30">
                <h3 className="font-semibold text-foreground">المعلومات الأساسية</h3>
                <div className="space-y-2">
                  <Label>اسم القناة <span className="text-destructive">*</span></Label>
                  <Input
                    value={channelForm.name || ''}
                    onChange={(e) => setChannelForm(prev => ({ ...prev, name: e.target.value }))}
                    placeholder="مثال: beIN Sports HD"
                    className="bg-secondary border-border"
                  />
                </div>
                <ImageUploader
                  value={channelForm.imageUrl || ''}
                  onChange={(base64) => setChannelForm(prev => ({ ...prev, imageUrl: base64 }))}
                  label="صورة القناة"
                />
              </div>

              {/* Platform-Specific Settings with Tabs */}
              <Tabs defaultValue="web" className="w-full">
                <TabsList className="grid w-full grid-cols-2 bg-muted">
                  <TabsTrigger value="web" className="flex items-center gap-2">
                    <Globe className="w-4 h-4" />
                    🌐 إعدادات الويب
                  </TabsTrigger>
                  <TabsTrigger value="android" className="flex items-center gap-2">
                    <Smartphone className="w-4 h-4" />
                    📱 إعدادات أندرويد
                  </TabsTrigger>
                </TabsList>
                
                <TabsContent value="web" className="mt-4">
                  <WebConfigForm
                    streamConfig={channelForm.stream}
                    playerType={channelForm.preferredPlayer}
                    iosPlayerApp={channelForm.iosPlayerApp}
                    onStreamChange={(stream) => setChannelForm(prev => ({ ...prev, stream }))}
                    onPlayerTypeChange={(playerType) => setChannelForm(prev => ({ ...prev, preferredPlayer: playerType }))}
                    onIosPlayerAppChange={(app) => setChannelForm(prev => ({ ...prev, iosPlayerApp: app }))}
                  />
                </TabsContent>
                
                <TabsContent value="android" className="mt-4">
                  <AndroidConfigForm
                    config={channelForm.androidStream}
                    actionType={channelForm.androidActionType}
                    onChange={(config) => setChannelForm(prev => ({ ...prev, androidStream: config }))}
                    onActionTypeChange={(actionType) => setChannelForm(prev => ({ ...prev, androidActionType: actionType }))}
                  />
                </TabsContent>
              </Tabs>
            </div>
          </ScrollArea>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">إلغاء</Button>
            </DialogClose>
            <Button onClick={handleSaveChannel} className="bg-primary text-primary-foreground">
              {editingChannel.channel ? 'حفظ التغييرات' : 'إضافة القناة'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  );
};

export default SideMenuManager;
