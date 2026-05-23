package com.qiuqiuqiu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class DbHelper extends SQLiteOpenHelper {
    static final String DB_NAME = "xiaoqiu_accounting.db";
    static final int DB_VERSION = 4;

    DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            ensureColumn(db, "accounts", "initial_balance", "INTEGER DEFAULT 0");
        }
        if (oldVersion < 3) {
            seedMissingCategories(db);
        }
        if (oldVersion < 4) {
            createIndexes(db);
        }
    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL,type TEXT NOT NULL,icon TEXT,color TEXT," +
                "sort_order INTEGER DEFAULT 0,is_enabled INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE accounts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL,type TEXT NOT NULL,initial_balance INTEGER DEFAULT 0,current_balance INTEGER DEFAULT 0," +
                "color TEXT,is_enabled INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT NOT NULL,amount INTEGER NOT NULL,category_id INTEGER," +
                "account_id INTEGER,target_account_id INTEGER,transaction_time INTEGER NOT NULL," +
                "note TEXT,reimburse_status TEXT DEFAULT 'none',created_at INTEGER,updated_at INTEGER,is_deleted INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE attachments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,transaction_id INTEGER NOT NULL,file_path TEXT NOT NULL,created_at INTEGER)");
        db.execSQL("CREATE TABLE budgets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT,budget_type TEXT NOT NULL,category_id INTEGER,month TEXT NOT NULL,amount INTEGER NOT NULL)");
        createIndexes(db);
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_deleted_time ON transactions(is_deleted, transaction_time)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_type_time ON transactions(type, transaction_time)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_category_time ON transactions(category_id, transaction_time)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_attachments_transaction ON attachments(transaction_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_budgets_type_month ON budgets(budget_type, month)");
    }

    private void ensureColumn(SQLiteDatabase db, String table, String column, String definition) {
        Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
        try {
            while (c.moveToNext()) {
                if (column.equals(c.getString(1))) return;
            }
        } finally {
            c.close();
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    private void seed(SQLiteDatabase db) {
        seedMissingCategories(db);
        insertAccount(db, "现金", "cash", 0, "#607D8B");
        insertAccount(db, "微信", "wechat", 0, "#2F7D68");
        insertAccount(db, "支付宝", "alipay", 0, "#1677FF");
        insertAccount(db, "银行卡", "bank", 0, "#4056A1");
        insertAccount(db, "信用卡", "credit", 0, "#C84B31");
    }

    private void seedMissingCategories(SQLiteDatabase db) {
        String[][] expense = {
                {"餐饮", "食", "#E85D4F"}, {"购物", "购", "#D7833F"}, {"日用", "日", "#9A7B4F"}, {"交通", "行", "#3E7CB1"},
                {"蔬菜", "蔬", "#3FA47B"}, {"水果", "果", "#66A85F"}, {"零食", "零", "#C99700"}, {"运动", "动", "#2F9AA0"},
                {"娱乐", "乐", "#8E63B0"}, {"通讯", "讯", "#667085"}, {"服饰", "衣", "#D95F8A"}, {"美容", "美", "#D783A8"},
                {"住房", "住", "#6C7A89"}, {"居家", "家", "#8A795D"}, {"孩子", "孩", "#4F79D9"}, {"长辈", "长", "#7B6EA8"},
                {"社交", "友", "#D95F8A"}, {"旅行", "旅", "#2F9AA0"}, {"烟酒", "酒", "#80664E"}, {"数码", "数", "#607D8B"},
                {"汽车", "车", "#4056A1"}, {"医疗", "医", "#3FA47B"}, {"书籍", "书", "#4F79D9"}, {"学习", "学", "#4F79D9"},
                {"宠物", "宠", "#A16B4F"}, {"礼金", "金", "#C99700"}, {"礼物", "礼", "#D95F8A"}, {"办公", "办", "#607D8B"},
                {"维修", "修", "#777777"}, {"捐赠", "捐", "#D95F8A"}, {"彩票", "彩", "#C99700"}, {"亲友", "亲", "#8E63B0"},
                {"人情", "情", "#D95F8A"}, {"生活", "生", "#9A7B4F"}, {"其他", "其", "#777777"}
        };
        String[][] income = {
                {"工资", "工", "#2F7D68"}, {"兼职", "兼", "#4A78C2"}, {"理财", "财", "#228B55"}, {"礼金", "礼", "#C99700"},
                {"奖金", "奖", "#C99700"}, {"红包", "红", "#D64545"}, {"报销", "报", "#607D8B"}, {"退款", "退", "#5B8DEF"},
                {"其他", "其", "#777777"}
        };
        for (int i = 0; i < expense.length; i++) ensureCategory(db, expense[i][0], "expense", expense[i][1], expense[i][2], i);
        for (int i = 0; i < income.length; i++) ensureCategory(db, income[i][0], "income", income[i][1], income[i][2], i);
    }

    private void ensureCategory(SQLiteDatabase db, String name, String type, String icon, String color, int order) {
        Cursor c = db.rawQuery("SELECT id FROM categories WHERE name=? AND type=? LIMIT 1", new String[]{name, type});
        try {
            if (c.moveToFirst()) return;
        } finally {
            c.close();
        }
        insertCategory(db, name, type, icon, color, order);
    }

    long addCategory(String name, String type) {
        return insertCategory(getWritableDatabase(), name, type, name.substring(0, 1), "#2F7D68", categories(type).size() + 1);
    }

    long addAccount(String name, long initialBalance) {
        return insertAccount(getWritableDatabase(), name, "other", initialBalance, "#2F7D68");
    }

    private long insertCategory(SQLiteDatabase db, String name, String type, String icon, String color, int order) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("type", type);
        v.put("icon", icon);
        v.put("color", color);
        v.put("sort_order", order);
        return db.insert("categories", null, v);
    }

    private long insertAccount(SQLiteDatabase db, String name, String type, long balance, String color) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("type", type);
        v.put("initial_balance", balance);
        v.put("current_balance", balance);
        v.put("color", color);
        return db.insert("accounts", null, v);
    }

    List<Item> categories(String type) {
        return queryItems("SELECT id,name,icon,color,0 FROM categories WHERE type=? AND is_enabled=1 ORDER BY sort_order,id", new String[]{type});
    }

    List<Item> accounts() {
        return queryItems("SELECT id,name,type,color,current_balance FROM accounts WHERE is_enabled=1 ORDER BY id", null);
    }

    private List<Item> queryItems(String sql, String[] args) {
        ArrayList<Item> items = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(sql, args);
        try {
            while (c.moveToNext()) {
                items.add(new Item(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4)));
            }
        } finally {
            c.close();
        }
        return items;
    }

    long addTransaction(String type, long amount, long categoryId, long accountId, long targetAccountId, long time, String note, String reimburse) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            long now = System.currentTimeMillis();
            v.put("type", type);
            v.put("amount", amount);
            if (categoryId > 0) v.put("category_id", categoryId);
            v.put("account_id", accountId);
            if (targetAccountId > 0) v.put("target_account_id", targetAccountId);
            v.put("transaction_time", time);
            v.put("note", note);
            v.put("reimburse_status", reimburse);
            v.put("created_at", now);
            v.put("updated_at", now);
            long id = db.insert("transactions", null, v);
            applyAccountDelta(db, type, amount, accountId, targetAccountId, 1);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    TxRecord transactionRecord(long id) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,type,amount,COALESCE(category_id,0),account_id,COALESCE(target_account_id,0),transaction_time,COALESCE(note,''),COALESCE(reimburse_status,'none') FROM transactions WHERE id=? AND is_deleted=0 LIMIT 1",
                new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) {
                return new TxRecord(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3), c.getLong(4),
                        c.getLong(5), c.getLong(6), c.getString(7), c.getString(8));
            }
            return null;
        } finally {
            c.close();
        }
    }

    void updateTransaction(long id, String type, long amount, long categoryId, long accountId, long targetAccountId, long time, String note, String reimburse) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT type,amount,account_id,COALESCE(target_account_id,0) FROM transactions WHERE id=? AND is_deleted=0", new String[]{String.valueOf(id)});
            try {
                if (c.moveToFirst()) applyAccountDelta(db, c.getString(0), c.getLong(1), c.getLong(2), c.getLong(3), -1);
            } finally {
                c.close();
            }
            ContentValues v = new ContentValues();
            v.put("type", type);
            v.put("amount", amount);
            if (categoryId > 0) v.put("category_id", categoryId);
            else v.putNull("category_id");
            v.put("account_id", accountId);
            if (targetAccountId > 0) v.put("target_account_id", targetAccountId);
            else v.putNull("target_account_id");
            v.put("transaction_time", time);
            v.put("note", note);
            v.put("reimburse_status", reimburse);
            v.put("updated_at", System.currentTimeMillis());
            db.update("transactions", v, "id=?", new String[]{String.valueOf(id)});
            applyAccountDelta(db, type, amount, accountId, targetAccountId, 1);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void addAttachment(long transactionId, String path) {
        ContentValues v = new ContentValues();
        v.put("transaction_id", transactionId);
        v.put("file_path", path);
        v.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insert("attachments", null, v);
    }

    List<String> attachmentPaths(long transactionId) {
        ArrayList<String> paths = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT file_path FROM attachments WHERE transaction_id=? ORDER BY id",
                new String[]{String.valueOf(transactionId)});
        try {
            while (c.moveToNext()) paths.add(c.getString(0));
        } finally {
            c.close();
        }
        return paths;
    }

    List<Attachment> allAttachments() {
        ArrayList<Attachment> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,file_path FROM attachments ORDER BY id", null);
        try {
            while (c.moveToNext()) list.add(new Attachment(c.getLong(0), c.getString(1)));
        } finally {
            c.close();
        }
        return list;
    }

    void updateAttachmentPath(long id, String path) {
        ContentValues v = new ContentValues();
        v.put("file_path", path);
        getWritableDatabase().update("attachments", v, "id=?", new String[]{String.valueOf(id)});
    }

    void normalizeAttachmentPaths(String receiptsDir) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT id,file_path FROM attachments ORDER BY id", null);
        try {
            while (c.moveToNext()) {
                String oldPath = c.getString(1);
                if (oldPath == null || oldPath.length() == 0) continue;
                String name = oldPath.replace("\\", "/");
                int slash = name.lastIndexOf('/');
                if (slash >= 0) name = name.substring(slash + 1);
                ContentValues v = new ContentValues();
                v.put("file_path", receiptsDir + "/" + name);
                db.update("attachments", v, "id=?", new String[]{String.valueOf(c.getLong(0))});
            }
        } finally {
            c.close();
        }
    }

    void deleteTransaction(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT type,amount,account_id,COALESCE(target_account_id,0) FROM transactions WHERE id=? AND is_deleted=0", new String[]{String.valueOf(id)});
            try {
                if (c.moveToFirst()) {
                    applyAccountDelta(db, c.getString(0), c.getLong(1), c.getLong(2), c.getLong(3), -1);
                    ContentValues v = new ContentValues();
                    v.put("is_deleted", 1);
                    v.put("updated_at", System.currentTimeMillis());
                    db.update("transactions", v, "id=?", new String[]{String.valueOf(id)});
                }
            } finally {
                c.close();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void applyAccountDelta(SQLiteDatabase db, String type, long amount, long accountId, long targetAccountId, int direction) {
        if ("expense".equals(type)) changeBalance(db, accountId, -amount * direction);
        if ("income".equals(type)) changeBalance(db, accountId, amount * direction);
        if ("transfer".equals(type)) {
            changeBalance(db, accountId, -amount * direction);
            changeBalance(db, targetAccountId, amount * direction);
        }
    }

    private void changeBalance(SQLiteDatabase db, long accountId, long delta) {
        db.execSQL("UPDATE accounts SET current_balance=current_balance+? WHERE id=?", new Object[]{delta, accountId});
    }

    List<Tx> transactions(String keyword, String typeFilter, long start, long end) {
        ArrayList<Tx> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.id,t.type,t.amount,t.transaction_time,COALESCE(t.note,''),");
        sql.append("COALESCE(c.name,'转账'),COALESCE(c.icon,'转'),COALESCE(a.name,''),");
        sql.append("(SELECT COUNT(*) FROM attachments x WHERE x.transaction_id=t.id),COALESCE(t.reimburse_status,'none') ");
        sql.append("FROM transactions t LEFT JOIN categories c ON c.id=t.category_id ");
        sql.append("LEFT JOIN accounts a ON a.id=t.account_id WHERE t.is_deleted=0 ");
        ArrayList<String> args = new ArrayList<>();
        if (start > 0) {
            sql.append("AND t.transaction_time>=? ");
            args.add(String.valueOf(start));
        }
        if (end > 0) {
            sql.append("AND t.transaction_time<? ");
            args.add(String.valueOf(end));
        }
        if (keyword != null && keyword.trim().length() > 0) {
            sql.append("AND (t.note LIKE ? OR c.name LIKE ? OR a.name LIKE ?) ");
            String k = "%" + keyword.trim() + "%";
            args.add(k);
            args.add(k);
            args.add(k);
        }
        if (typeFilter != null && typeFilter.length() > 0) {
            sql.append("AND t.type=? ");
            args.add(typeFilter);
        }
        sql.append("ORDER BY t.transaction_time DESC,t.id DESC LIMIT 500");
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (c.moveToNext()) {
                list.add(new Tx(c.getLong(0), c.getString(1), c.getLong(2), c.getLong(3), c.getString(4),
                        c.getString(5), c.getString(6), c.getString(7), c.getInt(8), c.getString(9)));
            }
        } finally {
            c.close();
        }
        return list;
    }

    long sum(String type, long start, long end) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE is_deleted=0 AND type=? ");
        ArrayList<String> args = new ArrayList<>();
        args.add(type);
        if (start > 0) {
            sql.append("AND transaction_time>=? ");
            args.add(String.valueOf(start));
        }
        if (end > 0) {
            sql.append("AND transaction_time<? ");
            args.add(String.valueOf(end));
        }
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            return c.moveToFirst() ? c.getLong(0) : 0;
        } finally {
            c.close();
        }
    }

    long count(String type, long start, long end) {
        return aggregate("COUNT(*)", type, start, end);
    }

    long maxAmount(String type, long start, long end) {
        return aggregate("COALESCE(MAX(amount),0)", type, start, end);
    }

    private long aggregate(String expression, String type, long start, long end) {
        StringBuilder sql = new StringBuilder("SELECT " + expression + " FROM transactions WHERE is_deleted=0 AND type=? ");
        ArrayList<String> args = new ArrayList<>();
        args.add(type);
        if (start > 0) {
            sql.append("AND transaction_time>=? ");
            args.add(String.valueOf(start));
        }
        if (end > 0) {
            sql.append("AND transaction_time<? ");
            args.add(String.valueOf(end));
        }
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            return c.moveToFirst() ? c.getLong(0) : 0;
        } finally {
            c.close();
        }
    }

    List<Stat> categoryStats(long start, long end) {
        return categoryStats("expense", start, end);
    }

    List<Stat> categoryStats(String type, long start, long end) {
        ArrayList<Stat> stats = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.name,c.icon,COALESCE(SUM(t.amount),0) total FROM transactions t ");
        sql.append("LEFT JOIN categories c ON c.id=t.category_id WHERE t.is_deleted=0 AND t.type=? ");
        ArrayList<String> args = new ArrayList<>();
        args.add(type);
        if (start > 0) {
            sql.append("AND t.transaction_time>=? ");
            args.add(String.valueOf(start));
        }
        if (end > 0) {
            sql.append("AND t.transaction_time<? ");
            args.add(String.valueOf(end));
        }
        sql.append("GROUP BY c.id ORDER BY total DESC LIMIT 10");
        Cursor c = getReadableDatabase().rawQuery(sql.toString(), args.toArray(new String[0]));
        try {
            while (c.moveToNext()) stats.add(new Stat(c.getString(0), c.getString(1), c.getLong(2)));
        } finally {
            c.close();
        }
        return stats;
    }

    long budget(String month) {
        Cursor c = getReadableDatabase().rawQuery("SELECT amount FROM budgets WHERE budget_type='total' AND month=? LIMIT 1", new String[]{month});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0;
        } finally {
            c.close();
        }
    }

    void saveBudget(String month, long amount) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("budget_type", "total");
        v.put("month", month);
        v.put("amount", amount);
        int updated = db.update("budgets", v, "budget_type='total' AND month=?", new String[]{month});
        if (updated == 0) db.insert("budgets", null, v);
    }

    String exportCsv() {
        StringBuilder out = new StringBuilder("类型,金额,分类,账户,时间,备注\n");
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        for (Tx tx : transactions(null, null, 0, 0)) {
            out.append(typeName(tx.type)).append(',')
                    .append(formatMoney(tx.amount)).append(',')
                    .append(tx.category).append(',')
                    .append(tx.account).append(',')
                    .append(f.format(new Date(tx.time))).append(',')
                    .append(tx.note == null ? "" : tx.note.replace(",", " ")).append('\n');
        }
        return out.toString();
    }

    static String typeName(String type) {
        if ("income".equals(type)) return "收入";
        if ("expense".equals(type)) return "支出";
        return "转账";
    }

    static String formatMoney(long cents) {
        return String.format(Locale.CHINA, "%.2f", cents / 100.0);
    }

    static class Item {
        final long id;
        final String name;
        final String icon;
        final String color;
        final long balance;

        Item(long id, String name, String icon, String color, long balance) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.color = color;
            this.balance = balance;
        }
    }

    static class Tx {
        final long id, amount, time;
        final String type, note, category, icon, account, reimburse;
        final int attachments;

        Tx(long id, String type, long amount, long time, String note, String category, String icon, String account, int attachments, String reimburse) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.time = time;
            this.note = note;
            this.category = category;
            this.icon = icon;
            this.account = account;
            this.attachments = attachments;
            this.reimburse = reimburse;
        }
    }

    static class TxRecord {
        final long id, amount, categoryId, accountId, targetAccountId, time;
        final String type, note, reimburse;

        TxRecord(long id, String type, long amount, long categoryId, long accountId, long targetAccountId, long time, String note, String reimburse) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.categoryId = categoryId;
            this.accountId = accountId;
            this.targetAccountId = targetAccountId;
            this.time = time;
            this.note = note;
            this.reimburse = reimburse;
        }
    }

    static class Stat {
        final String name, icon;
        final long amount;

        Stat(String name, String icon, long amount) {
            this.name = name;
            this.icon = icon;
            this.amount = amount;
        }
    }

    static class Attachment {
        final long id;
        final String path;

        Attachment(long id, String path) {
            this.id = id;
            this.path = path;
        }
    }
}
