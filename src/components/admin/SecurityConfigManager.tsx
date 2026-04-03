import React, { useState, useEffect, useCallback } from 'react';
import { ref, get, set, push, remove } from 'firebase/database';
import { db } from '@/lib/firebase';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Shield, Plus, Trash2, Copy, Check } from 'lucide-react';
import { toast } from 'sonner';

interface SignatureEntry {
  id: string;
  hash: string;
  label: string;
  addedAt: number;
}

const SecurityConfigManager: React.FC = () => {
  const [signatures, setSignatures] = useState<SignatureEntry[]>([]);
  const [newHash, setNewHash] = useState('');
  const [newLabel, setNewLabel] = useState('');
  const [loading, setLoading] = useState(true);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const loadSignatures = useCallback(async () => {
    try {
      const snapshot = await get(ref(db, 'security/allowedSignatures'));
      if (snapshot.exists()) {
        const data = snapshot.val();
        const list: SignatureEntry[] = Object.entries(data).map(([id, val]: [string, any]) => ({
          id,
          hash: val.hash || '',
          label: val.label || '',
          addedAt: val.addedAt || 0,
        }));
        setSignatures(list.sort((a, b) => b.addedAt - a.addedAt));
      } else {
        setSignatures([]);
      }
    } catch (e) {
      toast.error('فشل تحميل البصمات');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadSignatures(); }, [loadSignatures]);

  const addSignature = async () => {
    const hash = newHash.trim().toLowerCase();
    if (!hash) { toast.error('أدخل بصمة SHA-256'); return; }
    if (hash.length !== 64 || !/^[a-f0-9]+$/.test(hash)) {
      toast.error('بصمة SHA-256 غير صالحة (يجب أن تكون 64 حرف hex)');
      return;
    }
    if (signatures.some(s => s.hash === hash)) {
      toast.error('هذه البصمة موجودة بالفعل');
      return;
    }

    try {
      const newRef = push(ref(db, 'security/allowedSignatures'));
      await set(newRef, {
        hash,
        label: newLabel.trim() || 'APK Build',
        addedAt: Date.now(),
      });
      setNewHash('');
      setNewLabel('');
      toast.success('تم إضافة البصمة');
      loadSignatures();
    } catch (e) {
      toast.error('فشل إضافة البصمة');
    }
  };

  const removeSignature = async (id: string) => {
    try {
      await remove(ref(db, `security/allowedSignatures/${id}`));
      toast.success('تم حذف البصمة');
      loadSignatures();
    } catch (e) {
      toast.error('فشل حذف البصمة');
    }
  };

  const copyHash = (hash: string, id: string) => {
    navigator.clipboard.writeText(hash);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Card className="bg-card border-border">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-foreground">
            <Shield className="w-5 h-5 text-primary" />
            بصمات التطبيق المسموح بها (SHA-256)
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            أضف بصمات SHA-256 للنسخ المصرح بها من التطبيق. التطبيق يتحقق ديناميكياً من هذه البصمات.
          </p>

          {/* Add new signature */}
          <div className="flex flex-col gap-2">
            <Input
              placeholder="SHA-256 Hash (64 hex chars)"
              value={newHash}
              onChange={e => setNewHash(e.target.value)}
              className="font-mono text-xs"
              dir="ltr"
            />
            <div className="flex gap-2">
              <Input
                placeholder="التسمية (اختياري)"
                value={newLabel}
                onChange={e => setNewLabel(e.target.value)}
                className="flex-1"
              />
              <Button onClick={addSignature} size="sm">
                <Plus className="w-4 h-4 mr-1" />
                إضافة
              </Button>
            </div>
          </div>

          {/* Signatures list */}
          <div className="space-y-2">
            {signatures.length === 0 ? (
              <p className="text-center text-muted-foreground py-4">
                لا توجد بصمات. التطبيق سيقبل أي نسخة.
              </p>
            ) : (
              signatures.map(sig => (
                <div
                  key={sig.id}
                  className="flex items-center justify-between gap-2 p-3 rounded-lg bg-secondary/50 border border-border"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground">{sig.label}</p>
                    <p className="text-xs font-mono text-muted-foreground truncate" dir="ltr">
                      {sig.hash}
                    </p>
                  </div>
                  <div className="flex items-center gap-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => copyHash(sig.hash, sig.id)}
                    >
                      {copiedId === sig.id ? (
                        <Check className="w-4 h-4 text-green-500" />
                      ) : (
                        <Copy className="w-4 h-4" />
                      )}
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => removeSignature(sig.id)}
                      className="hover:text-destructive"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default SecurityConfigManager;
