import React from 'react';
import { StreamConfig, DRMConfig, ClearKeyMode } from '@/types/admin';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Separator } from '@/components/ui/separator';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Link, Globe, Cookie, Shield, Key, LinkIcon } from 'lucide-react';

interface PlayerConfigFormProps {
  streamConfig: StreamConfig;
  onChange: (config: StreamConfig) => void;
}

const PlayerConfigForm: React.FC<PlayerConfigFormProps> = ({ streamConfig, onChange }) => {
  const updateField = <K extends keyof StreamConfig>(field: K, value: StreamConfig[K]) => {
    onChange({ ...streamConfig, [field]: value });
  };

  const updateDRM = <K extends keyof DRMConfig>(field: K, value: DRMConfig[K]) => {
    onChange({
      ...streamConfig,
      drm: { ...streamConfig.drm, [field]: value }
    });
  };

  const clearKeyMode = streamConfig.drm?.clearKeyMode || 'separate';

  return (
    <div className="space-y-6">
      <div className="space-y-4">
        <h4 className="font-semibold text-foreground flex items-center gap-2">
          <Link className="w-4 h-4 text-primary" />
          إعدادات البث
        </h4>
        
        <div className="space-y-2">
          <Label>رابط البث <span className="text-destructive">*</span></Label>
          <Input
            value={streamConfig.url || ''}
            onChange={(e) => updateField('url', e.target.value)}
            placeholder="https://example.com/stream.m3u8 or .mpd"
            className="bg-secondary border-border font-mono text-sm"
          />
          <p className="text-xs text-muted-foreground">يدعم .m3u8 (HLS) و .mpd (DASH)</p>
        </div>
      </div>

      <Separator className="bg-border" />

      <div className="space-y-4">
        <h4 className="font-semibold text-foreground flex items-center gap-2">
          <Globe className="w-4 h-4 text-primary" />
          ترويسات الطلب (اختياري)
        </h4>
        
        <div className="space-y-2">
          <Label>User-Agent</Label>
          <Input
            value={streamConfig.userAgent || ''}
            onChange={(e) => updateField('userAgent', e.target.value)}
            placeholder="Mozilla/5.0 (Windows NT 10.0; Win64; x64)..."
            className="bg-secondary border-border font-mono text-sm"
          />
        </div>

        <div className="space-y-2">
          <Label>Referrer</Label>
          <Input
            value={streamConfig.referrer || ''}
            onChange={(e) => updateField('referrer', e.target.value)}
            placeholder="https://example.com/"
            className="bg-secondary border-border font-mono text-sm"
          />
        </div>
      </div>

      <Separator className="bg-border" />

      <div className="space-y-4">
        <h4 className="font-semibold text-foreground flex items-center gap-2">
          <Cookie className="w-4 h-4 text-primary" />
          الكوكيز (اختياري)
        </h4>
        
        <div className="space-y-2">
          <Label>Cookies</Label>
          <Textarea
            value={streamConfig.cookies || ''}
            onChange={(e) => updateField('cookies', e.target.value)}
            placeholder="session=abc123; token=xyz789"
            className="bg-secondary border-border font-mono text-sm min-h-[80px]"
          />
        </div>
      </div>

      <Separator className="bg-border" />

      <div className="space-y-4">
        <h4 className="font-semibold text-foreground flex items-center gap-2">
          <Shield className="w-4 h-4 text-primary" />
          إعدادات DRM / ClearKey (لبث MPD)
        </h4>

        {/* ClearKey Mode Selection */}
        <div className="space-y-3">
          <Label>طريقة إدخال ClearKey</Label>
          <RadioGroup
            value={clearKeyMode}
            onValueChange={(value: ClearKeyMode) => updateDRM('clearKeyMode', value)}
            className="flex flex-col gap-2"
          >
            <div className="flex items-center gap-2">
              <RadioGroupItem value="separate" id="ck_separate" />
              <Label htmlFor="ck_separate" className="flex items-center gap-2 cursor-pointer text-sm">
                <Key className="w-4 h-4 text-muted-foreground" />
                منفصل (Key ID + Key)
              </Label>
            </div>
            <div className="flex items-center gap-2">
              <RadioGroupItem value="combined" id="ck_combined" />
              <Label htmlFor="ck_combined" className="flex items-center gap-2 cursor-pointer text-sm">
                <Key className="w-4 h-4 text-muted-foreground" />
                مدمج (KeyID:Key)
              </Label>
            </div>
            <div className="flex items-center gap-2">
              <RadioGroupItem value="url" id="ck_url" />
              <Label htmlFor="ck_url" className="flex items-center gap-2 cursor-pointer text-sm">
                <LinkIcon className="w-4 h-4 text-muted-foreground" />
                رابط ديناميكي (URL)
              </Label>
            </div>
          </RadioGroup>
        </div>

        {/* Separate Fields Mode */}
        {clearKeyMode === 'separate' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>ClearKey ID</Label>
              <Input
                value={streamConfig.drm?.clearKeyId || ''}
                onChange={(e) => updateDRM('clearKeyId', e.target.value)}
                placeholder="Key ID (hex)"
                className="bg-secondary border-border font-mono text-sm"
              />
            </div>
            
            <div className="space-y-2">
              <Label>ClearKey Key</Label>
              <Input
                value={streamConfig.drm?.clearKeyKey || ''}
                onChange={(e) => updateDRM('clearKeyKey', e.target.value)}
                placeholder="Decryption Key (hex)"
                className="bg-secondary border-border font-mono text-sm"
              />
            </div>
          </div>
        )}

        {/* Combined Format Mode */}
        {clearKeyMode === 'combined' && (
          <div className="space-y-2">
            <Label>ClearKey (مدمج)</Label>
            <Input
              value={streamConfig.drm?.clearKeyCombined || ''}
              onChange={(e) => updateDRM('clearKeyCombined', e.target.value)}
              placeholder="ef34ae91b4f2415e...:243248d8de1ff8c7..."
              className="bg-secondary border-border font-mono text-sm"
            />
            <p className="text-xs text-muted-foreground">
              الصيغة: <code className="bg-muted px-1 rounded">KeyID:Key</code>
            </p>
          </div>
        )}

        {/* URL Mode */}
        {clearKeyMode === 'url' && (
          <div className="space-y-2">
            <Label>رابط ClearKey</Label>
            <Input
              value={streamConfig.drm?.clearKeyUrl || ''}
              onChange={(e) => updateDRM('clearKeyUrl', e.target.value)}
              placeholder="https://api.example.com/keys?id=..."
              className="bg-secondary border-border font-mono text-sm"
            />
            <p className="text-xs text-muted-foreground">
              رابط يُرجع المفاتيح بصيغة <code className="bg-muted px-1 rounded">KeyID:Key</code>
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default PlayerConfigForm;
