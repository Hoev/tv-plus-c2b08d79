import React from 'react';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Globe, Play, Monitor } from 'lucide-react';
import PlayerConfigForm from './PlayerConfigForm';
import type { StreamConfig, WebPlayerType } from '@/types/admin';

interface WebConfigFormProps {
  streamConfig: StreamConfig;
  playerType: WebPlayerType;
  onStreamChange: (config: StreamConfig) => void;
  onPlayerTypeChange: (playerType: WebPlayerType) => void;
}

const WebConfigForm: React.FC<WebConfigFormProps> = ({
  streamConfig,
  playerType,
  onStreamChange,
  onPlayerTypeChange
}) => {
  return (
    <div className="space-y-4 p-4 rounded-lg border-2 border-blue-600/30 bg-blue-950/20">
      <div className="flex items-center gap-2 text-blue-400">
        <Globe className="w-5 h-5" />
        <span className="font-bold text-base">🌐 إعدادات الويب</span>
      </div>
      
      {/* Player Engine Selection */}
      <div className="space-y-2">
        <Label>محرك التشغيل</Label>
        <Select
          value={playerType}
          onValueChange={(value: WebPlayerType) => onPlayerTypeChange(value)}
        >
          <SelectTrigger className="bg-secondary border-border">
            <SelectValue placeholder="اختر نوع المشغل" />
          </SelectTrigger>
          <SelectContent className="bg-popover border-border z-50">
            <SelectItem value="default">
              <div className="flex items-center gap-2">
                <Play className="w-4 h-4 text-green-500" />
                <span>المشغل الافتراضي (Native)</span>
              </div>
            </SelectItem>
            <SelectItem value="custom">
              <div className="flex items-center gap-2">
                <Globe className="w-4 h-4 text-blue-500" />
                <span>المشغل المخصص (Custom Player)</span>
              </div>
            </SelectItem>
            <SelectItem value="iframe">
              <div className="flex items-center gap-2">
                <Monitor className="w-4 h-4 text-purple-500" />
                <span>Web/Iframe (تضمين مباشر)</span>
              </div>
            </SelectItem>
          </SelectContent>
        </Select>
        <p className="text-xs text-muted-foreground">
          المستخدم لن يرى خيار التبديل بين المشغلات
        </p>
      </div>

      {/* Stream Configuration */}
      <PlayerConfigForm
        streamConfig={streamConfig}
        onChange={onStreamChange}
      />
    </div>
  );
};

export default WebConfigForm;
