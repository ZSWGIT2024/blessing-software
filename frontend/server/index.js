const express = require('express');
const multer = require('multer');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const sqlite3 = require('sqlite3').verbose();

// 初始化Express应用
const app = express();

// ========== 1. 基础配置 ==========
// 跨域配置
app.use(cors({
  origin: 'http://localhost:5173'
}));

// 中间件
app.use(express.json());
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// 上传目录配置
const uploadDir = path.join(__dirname, 'public', 'uploads');
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}

// ========== 2. 数据库初始化 ==========
const db = new sqlite3.Database('./database.db', (err) => {
  if (err) console.error(err.message);
  console.log('Connected to SQLite database.');
});

// 创建表结构
db.serialize(() => {
  // 用户表
  db.run(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      username TEXT NOT NULL,
      avatar_url TEXT,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )
  `);
  
  // 初始化默认用户
  db.run(`
    INSERT OR IGNORE INTO users (id, username, avatar_url)
    VALUES ('AC123456', '路人女主的养成方法', '/uploads/default-avatar.jpg')
  `);
  
  // 图片表
  db.run(`
    CREATE TABLE IF NOT EXISTS images (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      original_name TEXT NOT NULL,
      storage_path TEXT NOT NULL,
      mime_type TEXT NOT NULL,
      size INTEGER NOT NULL,
      title TEXT NOT NULL,               -- 新增
    artist TEXT NOT NULL,              -- 新增
      status TEXT DEFAULT 'pending',
      upload_date TEXT DEFAULT CURRENT_TIMESTAMP,
      review_date TEXT,
      description TEXT
    )
  `);
});

// ========== 3. 文件上传配置 ==========
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir);
  },
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname);
    cb(null, `${Date.now()}${ext}`);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 2 * 1024 * 1024 }, // 限制2MB
  fileFilter: (req, file, cb) => {
    if (['image/jpeg', 'image/png'].includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error('只支持JPEG/PNG格式'), false);
    }
  }
});

// ========== 新增验证中间件 ==========
// 可以放在路由控制器定义之前

const validateImageUpload = (req, res, next) => {
  if (!req.files || req.files.length === 0) {
    return res.status(400).json({ 
      error: 'NO_FILES',
      message: '未上传任何文件' 
    });
  }

  // 检查文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
  const invalidFiles = req.files.filter(
    f => !allowedTypes.includes(f.mimetype)
  );

  if (invalidFiles.length > 0) {
    return res.status(400).json({ 
      error: 'INVALID_FILE_TYPE',
      message: '包含不支持的文件类型',
      files: invalidFiles.map(f => ({
        name: f.originalname,
        type: f.mimetype
      }))
    });
  }

  // 检查文件大小 (multer已处理，这里做二次验证)
  const oversizedFiles = req.files.filter(
    f => f.size > 5 * 1024 * 1024 // 5MB
  );
  
  if (oversizedFiles.length > 0) {
    return res.status(400).json({
      error: 'FILE_TOO_LARGE',
      message: '文件大小超过限制',
      files: oversizedFiles.map(f => ({
        name: f.originalname,
        size: (f.size / 1024 / 1024).toFixed(2) + 'MB'
      }))
    });
  }

  next();
};

// ========== 4. 路由控制器 ==========
// 用户控制器
const userController = {
  getProfile: (req, res) => {
    const userId = req.query.userId || 'AC123456';
    
    db.get(
      `SELECT username, avatar_url FROM users WHERE id = ?`,
      [userId],
      (err, row) => {
        if (err) {
          console.error('数据库查询错误:', err);
          return res.status(500).json({ 
            success: false,
            error: "DB_ERROR",
            message: "数据库查询失败"
          });
        }

        if (!row) {
          return res.status(404).json({
            success: false,
            error: "USER_NOT_FOUND",
            message: `未找到用户ID为 ${userId} 的用户`
          });
        }

        res.json({
          success: true,
          data: {
            username: row.username,
            avatarUrl: row.avatar_url
          }
        });
      }
    );
  },

  updateUsername: (req, res) => {
    const { username, userId = 'AC123456' } = req.body;
    
    if (!username || username.trim().length < 2) {
      return res.status(400).json({ 
        success: false, 
        message: '用户名至少需要2个字符' 
      });
    }
    
    db.run(
      `UPDATE users SET username = ? WHERE id = ?`,
      [username, userId],
      function(err) {
        if (err) {
          console.error('数据库更新失败:', err);
          return res.status(500).json({ 
            success: false,
            message: '数据库更新失败'
          });
        }
        res.json({ 
          success: true, 
          message: '用户名更新成功',
          data: { username }
        });
      }
    );
  },

  uploadAvatar: async (req, res) => {
    try {
      if (!req.file) {
        return res.status(400).json({ error: 'NO_FILE_UPLOADED' });
      }
  
      const userId = req.body.userId || 'AC123456';
      const fileUrl = `/uploads/${req.file.filename}`;
  
      // 返回完整的可访问URL
      const fullUrl = `http://localhost:3001${fileUrl}`;
  
      db.run(
        `UPDATE users SET avatar_url = ? WHERE id = ?`,
        [fullUrl, userId], // 存储完整URL
        function(err) {
          if (err) {
            console.error('数据库更新失败:', err);
            return res.status(500).json({ error: 'DB_UPDATE_FAILED' });
          }
          res.json({ url: fullUrl }); // 返回完整URL
        }
      );
    } catch (err) {
      console.error('上传处理错误:', err);
      res.status(500).json({ error: 'SERVER_ERROR' });
    }
  }
};

// ========== 图片控制器 ==========
const imageController = {
  uploadImage: (req, res) => {
    const { title, artist, description } = req.body;
    
    // 验证必填字段
    if (!title?.trim()) {
      return res.status(400).json({
        success: false,
        error: 'MISSING_TITLE',
        message: '作品标题不能为空'
      });
    }

    // 处理多文件上传
    const uploadPromises = req.files.map(file => {
      return new Promise((resolve, reject) => {
        db
.run(
          `
INSERT INTO images (
            original_name, 
            storage_path, 
            mime_type, 
            size,
            title,
            artist,
            description
          ) VALUES (?, ?, ?, ?, ?, ?, ?)
`,
          [
            file
.originalname,
            `/uploads/${file.filename}`,
            file
.mimetype,
            file
.size,
            title
,
            artist 
|| '匿名',
            description 
|| ''
          ],
          function(err) {
            if (err) {
              console
.error('数据库插入错误:', err);
              return reject(err);
            }
            resolve({
              id: this.lastID,
              url: `/uploads/${file.filename}`,
              name: file.
originalname
            });
          }
        );
      });
    });

    Promise
.all(uploadPromises)
      .then(results => {
        res
.json({
          success: true,
          data: {
            items: results,
            count: results.length,
            totalSize: req.files.reduce((sum, f) => sum + f.size, 0)
          },
          meta: {
            timestamp: new Date().toISOString()
          }
        });
      })
      .catch(err => {
        console
.error('上传处理失败:', err);
        res
.status(500).json({
          success: false,
          error: 'DATABASE_ERROR',
          message: '文件信息保存失败'
        });
      });
  },
  getSubmissions: (req, res) => {
    const { status } = req.query;
    let query = "SELECT * FROM images";
    const params = [];
    
    if (status && status !== 'all') {
      query 
+= " WHERE status = ?";
      params
.push(status);
    }
    
    db
.all(query, params, (err, rows) => {
      if (err) {
        console
.error(err);
        return res.status(500).json({ success: false, message: 'Database error' });
      }
      res
.json({ 
        success: true,
        data: rows.map(item => ({
          ...item,
          artist: item.artist || '匿名用户'
        }))
      });
    });
  },
  getAllImages: (req, res) => {
    db.all("SELECT * FROM images", [], (err, rows) => {
      if (err) {
        console.error(err);
        return res.status(500).json({ success: false, message: 'Database error' });
      }
      res.json({ success: true, data: rows });
    });
  },

  getPendingImages: (req, res) => {
    db.all("SELECT * FROM images WHERE status = 'pending'", [], (err, rows) => {
      if (err) {
        console.error(err);
        return res.status(500).json({ success: false, message: 'Database error' });
      }
      res.json({ success: true, data: rows });
    });
  },

  reviewImage: (req, res) => {
    const { id } = req.params;
    const { status } = req.body;

    db.run(
      "UPDATE images SET status = ?, review_date = CURRENT_TIMESTAMP WHERE id = ?",
      [status, id],
      function(err) {
        if (err) {
          console.error(err);
          return res.status(500).json({ success: false, message: 'Database error' });
        }
        res.json({ success: true, message: 'Review status updated' });
      }
    );
  },

  getApprovedImages: (req, res) => {
    db.all("SELECT * FROM images WHERE status = 'approved'", [], (err, rows) => {
      if (err) {
        console.error(err);
        return res.status(500).json({ success: false, message: 'Database error' });
      }
      res.json({ success: true, data: rows });
    });
  }
};

// 放在其他路由之前
app.get('/api/health', (req, res) => {
  res.json({ 
    status: 'ok', 
    services: {
      database: true,
      storage: true
    },
    timestamp: new Date().toISOString()
  });
});
// ========== 5. 路由定义 ==========
// 用户路由
const userRouter = express.Router();
userRouter.get('/test', (req, res) => res.json({ message: "测试路由正常工作" }));
userRouter.get('/user-profile', userController.getProfile);
userRouter.post('/update-username', userController.updateUsername);
userRouter.post('/upload-avatar', upload.single('avatar'), userController.uploadAvatar);

// 图片路由
const imageRouter = express.Router();
imageRouter.post(
  '/upload',
  upload.array('images', 12), // 处理最多12个文件
  validateImageUpload,        // 添加验证中间件
  imageController.uploadImage // 控制器
);
imageRouter.get('/', imageController.getAllImages);
imageRouter.get('/submissions', imageController.getSubmissions); // 新增获取投稿接口
imageRouter.get('/pending', imageController.getPendingImages);
imageRouter.post('/:id/review', imageController.reviewImage);
imageRouter.get('/approved', imageController.getApprovedImages);

// API路由
const apiRouter = express.Router();
apiRouter.use('/users', userRouter);
apiRouter.use('/images', imageRouter);

// ========== 6. 路由挂载 ==========
app.use('/api', apiRouter);

// 静态文件服务
app.use('/uploads', express.static(path.join(__dirname, 'public', 'uploads')));

// ========== 7. 首页路由 ==========
app.get('/', (req, res) => {
  res.render('index', {
    title: 'API 文档中心',
    endpoints: [
      {
        method: 'GET',
        path: '/api/users/user-profile',
        desc: '获取用户基本信息',
        example: { 
          success: true,
          data: {
            username: "示例用户",
            avatarUrl: "/uploads/avatar.jpg"
          }
        }
      },
      {
        method: 'POST',
        path: '/api/images/upload',
        desc: '上传图片文件',
        example: {
          success: true,
          message: "文件上传成功",
          fileId: "abc123"
        }
      }
    ]
  });
});

// ========== 8. 错误处理 ==========
// 404处理
app.use((req, res) => {
  res.status(404).json({ success: false, message: 'Not Found' });
});

// 全局错误处理
// ========== 8. 错误处理 ==========
// 放在所有路由之后，app.listen之前

// 增强的错误日志中间件
app.use((err, req, res, next) => {
  // 详细错误日志
  console.error('请求错误详情:', {
    timestamp: new Date().toISOString(),
    path: req.path,
    method: req.method,
    params: req.params,
    query: req.query,
    body: req.body,
    error: err.stack || err.message
  });

  // 处理multer特定错误
  if (err instanceof multer.MulterError) {
    return res.status(400).json({
      error: err.code,
      message: err.message,
      type: 'UPLOAD_ERROR'
    });
  }

  // 处理验证错误
  if (err.message === 'INVALID_FILE_TYPE') {
    return res.status(400).json({
      error: 'VALIDATION_ERROR',
      message: '文件类型不支持'
    });
  }

  // 默认错误处理
  res.status(500).json({
    error: 'SERVER_ERROR',
    message: '服务器内部错误',
    requestId: req.id // 可以考虑添加请求ID
  });
});

// ========== 9. 启动服务器 ==========
const PORT = 3001;
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});



// server.js
// ├── 中间件配置
// │   ├── express.json()
// │   ├── cors()
// │   └── multer配置
// ├── 数据库初始化
// ├── 中间件定义
// │   ├── validateImageUpload
// │   └── 其他中间件
// ├── 控制器
// │   ├── userController
// │   └── imageController
// ├── 路由定义
// │   ├── userRouter
// │   └── imageRouter
// ├── 全局错误处理
// └── 启动服务器