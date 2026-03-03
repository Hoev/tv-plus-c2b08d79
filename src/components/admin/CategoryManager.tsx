import React, { useState, useEffect } from 'react';
import { ref, onValue, push, update, remove, set } from 'firebase/database';
import { db } from '@/lib/firebase';
import { Category } from '@/types/admin';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter, DialogClose } from '@/components/ui/dialog';
import { Plus, Edit2, Trash2, GripVertical, Folder, Eye, EyeOff } from 'lucide-react';

interface CategoryManagerProps {
  onSelectCategory: (category: Category | null) => void;
  selectedCategoryId: string | null;
}

const CategoryManager: React.FC<CategoryManagerProps> = ({ onSelectCategory, selectedCategoryId }) => {
  const [categories, setCategories] = useState<Record<string, Category>>({});
  const [newCategoryName, setNewCategoryName] = useState('');
  const [editCategory, setEditCategory] = useState<Category | null>(null);
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);

  useEffect(() => {
    const categoriesRef = ref(db, 'categories');
    const unsubscribe = onValue(
      categoriesRef,
      (snapshot) => {
        const data = (snapshot.val() || {}) as Record<string, Partial<Category>>;

        // Normalize IDs: in Realtime Database the object key is the true id.
        // This prevents cases where stored objects are missing `id`, which breaks
        // selection and causes writes to go to `categories/undefined/...`.
        const normalized: Record<string, Category> = Object.fromEntries(
          Object.entries(data).map(([id, cat]) => [
            id,
            {
              ...(cat as Category),
              id,
              channels: (cat as any)?.channels || {},
            },
          ])
        );

        setCategories(normalized);
      },
      (err) => {
        console.error('Firebase categories read error:', err);
        alert('تعذر تحميل الأقسام من Firebase. تأكد من صلاحيات قاعدة البيانات (Rules).');
      }
    );

    return () => unsubscribe();
  }, []);

  const handleAddCategory = async () => {
    if (!newCategoryName.trim()) return;
    
    try {
      const categoriesRef = ref(db, 'categories');
      const newRef = push(categoriesRef);
      const sortOrder = Object.keys(categories).length;
      
      await set(newRef, {
        id: newRef.key,
        name: newCategoryName.trim(),
        sortOrder,
        channels: {}
      });
      
      setNewCategoryName('');
      setIsAddDialogOpen(false);
    } catch (err) {
      console.error('Firebase add category error:', err);
      alert('فشل إضافة القسم. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  const handleUpdateCategory = async () => {
    if (!editCategory || !editCategory.name.trim()) return;
    
    try {
      const categoryRef = ref(db, `categories/${editCategory.id}`);
      await update(categoryRef, { name: editCategory.name.trim() });
      
      setEditCategory(null);
      setIsEditDialogOpen(false);
    } catch (err) {
      console.error('Firebase update category error:', err);
      alert('فشل تعديل القسم. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  const handleDeleteCategory = async (categoryId: string) => {
    if (!confirm('هل أنت متأكد أنك تريد حذف هذا القسم وجميع القنوات بداخله؟')) return;
    
    try {
      const categoryRef = ref(db, `categories/${categoryId}`);
      await remove(categoryRef);
      
      if (selectedCategoryId === categoryId) {
        onSelectCategory(null);
      }
    } catch (err) {
      console.error('Firebase delete category error:', err);
      alert('فشل حذف القسم. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  const handleMoveCategory = async (categoryId: string, direction: 'up' | 'down') => {
    const sortedCategories = Object.values(categories).sort((a, b) => a.sortOrder - b.sortOrder);
    const currentIndex = sortedCategories.findIndex(c => c.id === categoryId);
    
    try {
      if (direction === 'up' && currentIndex > 0) {
        const prevCategory = sortedCategories[currentIndex - 1];
        const currentCategory = sortedCategories[currentIndex];
        
        await update(ref(db, `categories/${categoryId}`), { sortOrder: prevCategory.sortOrder });
        await update(ref(db, `categories/${prevCategory.id}`), { sortOrder: currentCategory.sortOrder });
      } else if (direction === 'down' && currentIndex < sortedCategories.length - 1) {
        const nextCategory = sortedCategories[currentIndex + 1];
        const currentCategory = sortedCategories[currentIndex];
        
        await update(ref(db, `categories/${categoryId}`), { sortOrder: nextCategory.sortOrder });
        await update(ref(db, `categories/${nextCategory.id}`), { sortOrder: currentCategory.sortOrder });
      }
    } catch (err) {
      console.error('Firebase move category error:', err);
      alert('فشل تغيير ترتيب الأقسام. تأكد من صلاحيات الكتابة في Firebase (Rules).');
    }
  };

  const sortedCategories = Object.values(categories).sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <Card className="border-border bg-card">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg font-bold text-foreground">الأقسام</CardTitle>
        <Dialog open={isAddDialogOpen} onOpenChange={setIsAddDialogOpen}>
          <DialogTrigger asChild>
            <Button size="sm" className="bg-primary text-primary-foreground">
              <Plus className="w-4 h-4 mr-2" />
              إضافة
            </Button>
          </DialogTrigger>
          <DialogContent className="bg-card border-border">
            <DialogHeader>
              <DialogTitle>إضافة قسم جديد</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label>اسم القسم</Label>
                <Input
                  value={newCategoryName}
                  onChange={(e) => setNewCategoryName(e.target.value)}
                  placeholder="مثال: رياضة، أفلام، أخبار"
                  className="bg-secondary border-border"
                />
              </div>
            </div>
            <DialogFooter>
              <DialogClose asChild>
                <Button variant="outline">إلغاء</Button>
              </DialogClose>
              <Button onClick={handleAddCategory} className="bg-primary text-primary-foreground">
                إضافة القسم
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </CardHeader>
      <CardContent className="space-y-2">
        {sortedCategories.length === 0 ? (
          <p className="text-muted-foreground text-sm text-center py-4">لا توجد أقسام بعد</p>
        ) : (
          sortedCategories.map((category, index) => (
            <div
              key={category.id}
              className={`flex items-center gap-2 p-3 rounded-lg cursor-pointer transition-colors ${
                selectedCategoryId === category.id
                  ? 'bg-primary/20 border border-primary'
                  : 'bg-secondary hover:bg-secondary/80'
              }`}
              onClick={() => onSelectCategory(category)}
            >
              <div className="flex flex-col gap-0.5">
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-4 w-4 p-0"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleMoveCategory(category.id, 'up');
                  }}
                  disabled={index === 0}
                >
                  <GripVertical className="w-3 h-3 rotate-90" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-4 w-4 p-0"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleMoveCategory(category.id, 'down');
                  }}
                  disabled={index === sortedCategories.length - 1}
                >
                  <GripVertical className="w-3 h-3 rotate-90" />
                </Button>
              </div>
              <Folder className="w-5 h-5 text-primary" />
              <span className={`flex-1 font-medium ${category.hidden ? 'text-muted-foreground line-through' : 'text-foreground'}`}>
                {category.name}
                {category.hidden && <span className="text-xs text-amber-500 mr-2">(مخفي)</span>}
              </span>
              <span className="text-xs text-muted-foreground">
                {Object.keys(category.channels || {}).length} قناة
              </span>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                title={category.hidden ? 'إظهار القسم' : 'إخفاء القسم'}
                onClick={async (e) => {
                  e.stopPropagation();
                  try {
                    await update(ref(db, `categories/${category.id}`), { hidden: !category.hidden });
                  } catch (err) {
                    console.error('Toggle hidden error:', err);
                  }
                }}
              >
                {category.hidden ? <EyeOff className="w-4 h-4 text-amber-500" /> : <Eye className="w-4 h-4" />}
              </Button>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8"
                onClick={(e) => {
                  e.stopPropagation();
                  setEditCategory(category);
                  setIsEditDialogOpen(true);
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
                  handleDeleteCategory(category.id);
                }}
              >
                <Trash2 className="w-4 h-4" />
              </Button>
            </div>
          ))
        )}
      </CardContent>

      {/* Edit Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="bg-card border-border">
          <DialogHeader>
            <DialogTitle>تعديل القسم</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>اسم القسم</Label>
              <Input
                value={editCategory?.name || ''}
                onChange={(e) => setEditCategory(prev => prev ? { ...prev, name: e.target.value } : null)}
                className="bg-secondary border-border"
              />
            </div>
          </div>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">إلغاء</Button>
            </DialogClose>
            <Button onClick={handleUpdateCategory} className="bg-primary text-primary-foreground">
              حفظ التغييرات
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  );
};

export default CategoryManager;
