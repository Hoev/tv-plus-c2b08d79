import React, { useState, useEffect } from 'react';
import { ref, onValue, update } from 'firebase/database';
import { db } from '@/lib/firebase';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Switch } from '@/components/ui/switch';
import { Separator } from '@/components/ui/separator';
import type { AdConfig, Category } from '@/types/admin';

const AdConfigManager: React.FC = () => {
  const [adConfig, setAdConfig] = useState<AdConfig>({});
  const [categories, setCategories] = useState<Record<string, Category>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const unsubAds = onValue(ref(db, 'adConfig'), (snap) => {
      setAdConfig(snap.val() || {});
    });
    const unsubCats = onValue(ref(db, 'categories'), (snap) => {
      setCategories(snap.val() || {});
    });
    return () => { unsubAds(); unsubCats(); };
  }, []);

  const handleSaveAdConfig = async () => {
    setSaving(true);
    try {
      await update(ref(db, 'adConfig'), adConfig);
      alert('تم حفظ إعدادات الإعلانات بنجاح');
    } catch (err) {
      console.error('Error saving ad config:', err);
      alert('فشل حفظ الإعدادات');
    }
    setSaving(false);
  };

  const toggleAdGate = async (categoryId: string, enabled: boolean) => {
    try {
      await update(ref(db, `categories/${categoryId}`), { adGateEnabled: enabled });
    } catch (err) {
      console.error('Error toggling ad gate:', err);
      alert('فشل تحديث بوابة الإعلانات');
    }
  };

  const sortedCategories = Object.values(categories).sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <div className="space-y-6">
      {/* AdMob Configuration */}
      <Card className="border-border bg-card">
        <CardHeader>
          <CardTitle className="text-lg font-bold text-foreground flex items-center gap-2">
            📢 إعدادات AdMob
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center justify-between">
            <Label>تفعيل الإعلانات</Label>
            <Switch
              checked={adConfig.adsEnabled || false}
              onCheckedChange={(checked) => setAdConfig(prev => ({ ...prev, adsEnabled: checked }))}
            />
          </div>

          <Separator />

          <div className="space-y-2">
            <Label>Banner Ad Unit ID</Label>
            <Input
              value={adConfig.admobBannerId || ''}
              onChange={(e) => setAdConfig(prev => ({ ...prev, admobBannerId: e.target.value }))}
              placeholder="ca-app-pub-xxxxx/xxxxx"
              className="bg-secondary border-border font-mono text-sm"
              dir="ltr"
            />
          </div>

          <div className="space-y-2">
            <Label>Interstitial Ad Unit ID</Label>
            <Input
              value={adConfig.admobInterstitialId || ''}
              onChange={(e) => setAdConfig(prev => ({ ...prev, admobInterstitialId: e.target.value }))}
              placeholder="ca-app-pub-xxxxx/xxxxx"
              className="bg-secondary border-border font-mono text-sm"
              dir="ltr"
            />
          </div>

          <div className="space-y-2">
            <Label>Rewarded Video Ad Unit ID</Label>
            <Input
              value={adConfig.admobRewardedId || ''}
              onChange={(e) => setAdConfig(prev => ({ ...prev, admobRewardedId: e.target.value }))}
              placeholder="ca-app-pub-xxxxx/xxxxx"
              className="bg-secondary border-border font-mono text-sm"
              dir="ltr"
            />
            <p className="text-xs text-muted-foreground">
              يُستخدم لبوابة الإعلانات - يجب على المستخدم مشاهدة الإعلان قبل الوصول للقسم
            </p>
          </div>

          <Button
            onClick={handleSaveAdConfig}
            disabled={saving}
            className="w-full bg-primary text-primary-foreground"
          >
            {saving ? 'جارٍ الحفظ...' : 'حفظ إعدادات الإعلانات'}
          </Button>
        </CardContent>
      </Card>

      {/* Per-Section Ad Gate */}
      <Card className="border-border bg-card">
        <CardHeader>
          <CardTitle className="text-lg font-bold text-foreground flex items-center gap-2">
            🔒 بوابة الإعلانات لكل قسم
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <p className="text-sm text-muted-foreground mb-4">
            عند التفعيل، يجب على المستخدم مشاهدة إعلان (Rewarded Video) قبل الوصول إلى هذا القسم أو صفحاته الفرعية.
          </p>
          {sortedCategories.length === 0 ? (
            <p className="text-muted-foreground text-sm text-center py-4">لا توجد أقسام</p>
          ) : (
            sortedCategories.map((cat) => (
              <div
                key={cat.id}
                className="flex items-center justify-between p-3 rounded-lg bg-secondary"
              >
                <span className="font-medium text-foreground">{cat.name}</span>
                <Switch
                  checked={cat.adGateEnabled || false}
                  onCheckedChange={(checked) => toggleAdGate(cat.id, checked)}
                />
              </div>
            ))
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default AdConfigManager;
