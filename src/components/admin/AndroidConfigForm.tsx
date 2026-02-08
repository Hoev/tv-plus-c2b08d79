import React, { useState } from 'react';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Smartphone, Globe, Play, ExternalLink, FileCode, Key, Link } from 'lucide-react';
import type { AndroidStreamConfig, AndroidActionType, DrmScheme, ClearKeyMode } from '@/types/admin';

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
  // ClearKey input mode for Android DRM
  const [clearKeyMode, setClearKeyMode] = useState<ClearKeyMode>(
    config.drmClearKeyMode || 'combined'
  );

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
        <p className="text-xs text-muted-foreground">
          يدعم .m3u8 (HLS) و .mpd (DASH) و روابط الفيديو المباشرة
        </p>
      </div>

      {/* Intent URI (only for intent action type) */}
      {actionType === 'intent' && (
        <div className="space-y-2">
          <Label>Intent URI</Label>
          <Input
            value={config.intentUri || ''}
            onChange={(e) => updateConfig({ intentUri: e.target.value })}
            placeholder="intent://ako_player_exo#Intent;scheme=xmtv;package=com.mv.player;end"
            className="bg-secondary border-border font-mono text-xs"
            dir="ltr"
          />
          <p className="text-xs text-muted-foreground">
            لفتح تطبيق خارجي مثل MX Player أو VLC أو OTT Navigator
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
        <div className="space-y-4 pt-2 border-t border-border">
          <Label className="text-sm text-muted-foreground flex items-center gap-2">
            <FileCode className="w-4 h-4" />
            إعدادات DRM / ClearKey (لبث MPD)
          </Label>
          
          {/* DRM Scheme */}
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

          {/* ClearKey specific options */}
          {config.drmScheme === 'clearkey' && (
            <div className="space-y-3 p-3 rounded-lg bg-yellow-950/20 border border-yellow-600/30">
              <Label className="text-xs text-yellow-400 flex items-center gap-2">
                <Key className="w-3 h-3" />
                طريقة إدخال ClearKey
              </Label>
              
              <RadioGroup
                value={clearKeyMode}
                onValueChange={(value: ClearKeyMode) => {
                  setClearKeyMode(value);
                  updateConfig({ drmClearKeyMode: value });
                }}
                className="flex flex-col gap-2"
              >
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="separate" id="ck-separate" />
                  <Label htmlFor="ck-separate" className="text-xs cursor-pointer flex items-center gap-1">
                    <Key className="w-3 h-3" />
                    منفصل (Key ID + Key)
                  </Label>
                </div>
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="combined" id="ck-combined" />
                  <Label htmlFor="ck-combined" className="text-xs cursor-pointer flex items-center gap-1">
                    <Key className="w-3 h-3" />
                    مدمج (KeyID:Key)
                  </Label>
                </div>
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="url" id="ck-url" />
                  <Label htmlFor="ck-url" className="text-xs cursor-pointer flex items-center gap-1">
                    <Link className="w-3 h-3" />
                    رابط ديناميكي (URL)
                  </Label>
                </div>
              </RadioGroup>

              {/* Separate mode: Key ID + Key fields */}
              {clearKeyMode === 'separate' && (
                <div className="space-y-2">
                  <div className="space-y-1">
                    <Label className="text-xs">Key ID</Label>
                    <Input
                      value={config.drmKeyId || ''}
                      onChange={(e) => updateConfig({ drmKeyId: e.target.value })}
                      placeholder="b253c726c24c7c94a3ddf9b190f9fab6"
                      className="bg-secondary border-border font-mono text-xs"
                      dir="ltr"
                    />
                  </div>
                  <div className="space-y-1">
                    <Label className="text-xs">Key</Label>
                    <Input
                      value={config.drmKey || ''}
                      onChange={(e) => updateConfig({ drmKey: e.target.value })}
                      placeholder="L74G61HAn2BOm9HIn7QAXQ"
                      className="bg-secondary border-border font-mono text-xs"
                      dir="ltr"
                    />
                  </div>
                </div>
              )}

              {/* Combined mode: KeyID:Key format */}
              {clearKeyMode === 'combined' && (
                <div className="space-y-1">
                  <Label className="text-xs">ClearKey (مدمج)</Label>
                  <Input
                    value={config.drmClearKeyCombined || ''}
                    onChange={(e) => updateConfig({ drmClearKeyCombined: e.target.value })}
                    placeholder="b253c726c24c7c94a3ddf9b190f9fab6:L74G61HAn2BOm9HIn7QAXQ"
                    className="bg-secondary border-border font-mono text-xs"
                    dir="ltr"
                  />
                  <p className="text-xs text-muted-foreground">الصيغة: KeyID:Key</p>
                </div>
              )}

              {/* URL mode: Dynamic license URL */}
              {clearKeyMode === 'url' && (
                <div className="space-y-1">
                  <Label className="text-xs">رابط ClearKey</Label>
                  <Input
                    value={config.drmLicenseUrl || ''}
                    onChange={(e) => updateConfig({ drmLicenseUrl: e.target.value })}
                    placeholder="https://license.example.com/clearkey"
                    className="bg-secondary border-border font-mono text-xs"
                    dir="ltr"
                  />
                </div>
              )}
            </div>
          )}
          
          {/* Widevine/PlayReady License URL */}
          {(config.drmScheme === 'widevine' || config.drmScheme === 'playready') && (
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
          )}
        </div>
      )}
    </div>
  );
};

export default AndroidConfigForm;