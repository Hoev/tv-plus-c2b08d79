import React from 'react';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Smartphone, Globe, Play, ExternalLink, FileCode } from 'lucide-react';
import type { AndroidStreamConfig, AndroidActionType, DrmScheme } from '@/types/admin';

interface AndroidConfigFormProps {
  config: Partial<AndroidStreamConfig>;
  actionType: AndroidActionType;
  onChange: (config: Partial<AndroidStreamConfig>) => void;
  onActionTypeChange: (actionType: AndroidActionType) => void;
}

const AndroidConfigForm: React.FC<AndroidConfigFormProps> = ({
  config,
  actionType,
  onChange,
  onActionTypeChange
}) => {
  const updateConfig = (updates: Partial<AndroidStreamConfig>) => {
    onChange({ ...config, ...updates });
  };

  const updateHeaders = (field: string, value: string) => {
    onChange({
      ...config,
      headers: {
        ...config.headers,
        [field]: value
      }
    });
  };

  return (
    <div className="space-y-4 p-4 rounded-lg border-2 border-green-600/30 bg-green-950/20">
      <div className="flex items-center gap-2 text-green-400">
        <Smartphone className="w-5 h-5" />
        <span className="font-bold text-base">📱 إعدادات أندرويد</span>
      </div>
      
      {/* Action Type */}
      <div className="space-y-2">
        <Label>نوع الإجراء في التطبيق</Label>
        <Select
          value={actionType}
          onValueChange={(value: AndroidActionType) => onActionTypeChange(value)}
        >
          <SelectTrigger className="bg-secondary border-border">
            <SelectValue placeholder="اختر نوع الإجراء" />
          </SelectTrigger>
          <SelectContent className="bg-popover border-border z-50">
            <SelectItem value="native">
              <div className="flex items-center gap-2">
                <Play className="w-4 h-4 text-green-500" />
                <span>مشغل أصلي (Native Player)</span>
              </div>
            </SelectItem>
            <SelectItem value="webview">
              <div className="flex items-center gap-2">
                <Globe className="w-4 h-4 text-blue-500" />
                <span>WebView (تضمين ويب)</span>
              </div>
            </SelectItem>
            <SelectItem value="intent">
              <div className="flex items-center gap-2">
                <ExternalLink className="w-4 h-4 text-purple-500" />
                <span>Intent (تطبيق خارجي)</span>
              </div>
            </SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Stream URL */}
      <div className="space-y-2">
        <Label>رابط البث للتطبيق</Label>
        <Input
          value={config.url || ''}
          onChange={(e) => updateConfig({ url: e.target.value })}
          placeholder="https://stream.example.com/live.m3u8"
          className="bg-secondary border-border font-mono text-sm"
          dir="ltr"
        />
      </div>

      {/* Intent URI (only for intent action type) */}
      {actionType === 'intent' && (
        <div className="space-y-2">
          <Label>Intent URI</Label>
          <Input
            value={config.intentUri || ''}
            onChange={(e) => updateConfig({ intentUri: e.target.value })}
            placeholder="intent://example.com/#Intent;scheme=https;package=com.example;end"
            className="bg-secondary border-border font-mono text-xs"
            dir="ltr"
          />
          <p className="text-xs text-muted-foreground">
            لفتح تطبيق خارجي مثل MX Player أو VLC
          </p>
        </div>
      )}

      {/* Headers Section (for native and webview) */}
      {actionType !== 'intent' && (
        <div className="space-y-3 pt-2 border-t border-border">
          <Label className="text-sm text-muted-foreground">الهيدرز (Headers)</Label>
          
          <div className="grid grid-cols-1 gap-3">
            <div className="space-y-1">
              <Label className="text-xs">User-Agent</Label>
              <Input
                value={config.headers?.userAgent || ''}
                onChange={(e) => updateHeaders('userAgent', e.target.value)}
                placeholder="Mozilla/5.0..."
                className="bg-secondary border-border font-mono text-xs"
                dir="ltr"
              />
            </div>
            
            <div className="space-y-1">
              <Label className="text-xs">Referer</Label>
              <Input
                value={config.headers?.referrer || ''}
                onChange={(e) => updateHeaders('referrer', e.target.value)}
                placeholder="https://example.com"
                className="bg-secondary border-border font-mono text-xs"
                dir="ltr"
              />
            </div>
            
            <div className="space-y-1">
              <Label className="text-xs">Cookie</Label>
              <Input
                value={config.headers?.cookie || ''}
                onChange={(e) => updateHeaders('cookie', e.target.value)}
                placeholder="session=abc123; token=xyz"
                className="bg-secondary border-border font-mono text-xs"
                dir="ltr"
              />
            </div>
          </div>
        </div>
      )}

      {/* DRM Section (for native only) */}
      {actionType === 'native' && (
        <div className="space-y-3 pt-2 border-t border-border">
          <Label className="text-sm text-muted-foreground flex items-center gap-2">
            <FileCode className="w-4 h-4" />
            إعدادات DRM
          </Label>
          
          <div className="space-y-2">
            <Label className="text-xs">نوع DRM</Label>
            <Select
              value={config.drmScheme || 'clearkey'}
              onValueChange={(value: DrmScheme) => updateConfig({ drmScheme: value })}
            >
              <SelectTrigger className="bg-secondary border-border">
                <SelectValue />
              </SelectTrigger>
              <SelectContent className="bg-popover border-border z-50">
                <SelectItem value="clearkey">ClearKey</SelectItem>
                <SelectItem value="widevine">Widevine</SelectItem>
                <SelectItem value="playready">PlayReady</SelectItem>
              </SelectContent>
            </Select>
          </div>
          
          <div className="space-y-1">
            <Label className="text-xs">رابط الرخصة (License URL)</Label>
            <Input
              value={config.drmLicenseUrl || ''}
              onChange={(e) => updateConfig({ drmLicenseUrl: e.target.value })}
              placeholder="https://license.example.com/drm"
              className="bg-secondary border-border font-mono text-xs"
              dir="ltr"
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default AndroidConfigForm;
