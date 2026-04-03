import React, { useState, useEffect } from 'react';
import { useAdminAuth } from '@/hooks/useAdminAuth';
import AdminLogin from './AdminLogin';
import CategoryManager from '@/components/admin/CategoryManager';
import ChannelManager from '@/components/admin/ChannelManager';
import SideMenuManager from '@/components/admin/SideMenuManager';
import NotificationManager from '@/components/admin/NotificationManager';
import AdConfigManager from '@/components/admin/AdConfigManager';
import SecurityConfigManager from '@/components/admin/SecurityConfigManager';
import { Category } from '@/types/admin';
import { Button } from '@/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { LogOut, Settings, Tv, Menu, Folder, Shield, Bell, Megaphone, Lock } from 'lucide-react';

// Update manifest for admin PWA
const updateAdminManifest = () => {
  const existingManifest = document.querySelector('link[rel="manifest"]');
  if (existingManifest) {
    existingManifest.setAttribute('href', '/admin-manifest.json');
  }
};

const AdminDashboard: React.FC = () => {
  const { user, loading, isAuthorized, logout } = useAdminAuth();
  const [selectedCategory, setSelectedCategory] = useState<Category | null>(null);

  // Set admin manifest for PWA
  useEffect(() => {
    updateAdminManifest();
    // Update document title for admin
    document.title = 'TV Control - لوحة التحكم';
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin" />
          <p className="text-muted-foreground">جارٍ التحميل...</p>
        </div>
      </div>
    );
  }

  if (!user || !isAuthorized) {
    return <AdminLogin />;
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 bg-card border-b border-border">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <Shield className="w-5 h-5 text-primary" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-foreground">لوحة تحكم المشرف</h1>
                <p className="text-xs text-muted-foreground">{user.email}</p>
              </div>
            </div>
            <Button 
              variant="outline" 
              onClick={logout}
              className="border-border hover:bg-destructive hover:text-destructive-foreground hover:border-destructive"
            >
              <LogOut className="w-4 h-4 mr-2" />
              تسجيل الخروج
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="container mx-auto px-4 py-6">
        <Tabs defaultValue="categories" className="space-y-6">
          <TabsList className="bg-secondary border border-border w-full overflow-x-auto flex-nowrap justify-start">
            <TabsTrigger value="categories" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
              <Folder className="w-4 h-4 mr-2" />
              الأقسام
            </TabsTrigger>
            <TabsTrigger value="sidemenus" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
              <Menu className="w-4 h-4 mr-2" />
              القوائم الجانبية
            </TabsTrigger>
            <TabsTrigger value="notifications" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
              <Bell className="w-4 h-4 mr-2" />
              الإشعارات
            </TabsTrigger>
            <TabsTrigger value="ads" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
              <Megaphone className="w-4 h-4 mr-2" />
              الإعلانات
            </TabsTrigger>
            <TabsTrigger value="settings" className="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground">
              <Settings className="w-4 h-4 mr-2" />
              الإعدادات
            </TabsTrigger>
          </TabsList>

          <TabsContent value="categories" className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <div className="lg:col-span-1">
                <CategoryManager 
                  onSelectCategory={setSelectedCategory}
                  selectedCategoryId={selectedCategory?.id || null}
                />
              </div>
              <div className="lg:col-span-2">
                {selectedCategory ? (
                  <ChannelManager category={selectedCategory} />
                ) : (
                  <div className="border border-dashed border-border rounded-2xl p-12 text-center">
                    <Tv className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                    <p className="text-muted-foreground">اختر قسماً لإدارة قنواته</p>
                  </div>
                )}
              </div>
            </div>
          </TabsContent>

          <TabsContent value="sidemenus">
            <SideMenuManager />
          </TabsContent>

          <TabsContent value="notifications">
            <div className="max-w-2xl">
              <NotificationManager />
            </div>
          </TabsContent>

          <TabsContent value="ads">
            <div className="max-w-2xl">
              <AdConfigManager />
            </div>
          </TabsContent>

          <TabsContent value="settings">
            <div className="max-w-2xl">
              <div className="bg-card rounded-2xl p-6 border border-border space-y-6">
                <h3 className="text-lg font-bold text-foreground">إعدادات المشرف</h3>
                
                <div className="space-y-4">
                  <div className="flex items-center justify-between py-3 border-b border-border">
                    <span className="text-muted-foreground">مسجل الدخول كـ</span>
                    <span className="text-foreground font-medium">{user.email}</span>
                  </div>
                  <div className="flex items-center justify-between py-3 border-b border-border">
                    <span className="text-muted-foreground">قاعدة البيانات</span>
                    <span className="text-primary font-medium">Firebase Realtime Database</span>
                  </div>
                  <div className="flex items-center justify-between py-3 border-b border-border">
                    <span className="text-muted-foreground">تسجيل الدخول</span>
                    <span className="text-primary font-medium">Firebase Auth</span>
                  </div>
                  <div className="flex items-center justify-between py-3">
                    <span className="text-muted-foreground">إصدار اللوحة</span>
                    <span className="text-foreground font-medium">1.0.0</span>
                  </div>
                </div>

                <div className="pt-4">
                  <p className="text-xs text-muted-foreground">
                    هذه اللوحة مؤمنة ولا يمكن الوصول إليها إلا للحساب المخوّل.
                    يتم حفظ التغييرات في Firebase Realtime Database مباشرة.
                  </p>
                </div>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  );
};

export default AdminDashboard;
