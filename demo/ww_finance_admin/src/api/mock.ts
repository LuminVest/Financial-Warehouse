// Mock 数据 - 前端独立运行时使用
// 真实接口对接后，将 axios baseURL 指向后端即可

export interface PointLevel {
  id: number
  levelName: string
  minScore: number
  maxScore: number
  borrowLimit: number
  createTime: string
}

export const mockPointLevels: PointLevel[] = [
  { id: 1, levelName: '青铜', minScore: 10, maxScore: 50, borrowLimit: 10000, createTime: '2020-12-08 17:29' },
  { id: 2, levelName: '白银', minScore: 51, maxScore: 100, borrowLimit: 50000, createTime: '2020-12-08 17:42' },
  { id: 3, levelName: '黄金', minScore: 101, maxScore: 300, borrowLimit: 100000, createTime: '2020-12-08 17:57' },
  { id: 4, levelName: '钻石', minScore: 301, maxScore: 1000, borrowLimit: 300000, createTime: '2024-09-04 19:28' },
]

export interface Member {
  id: number
  phone: string
  nickname: string
  realName: string
  idCard: string
  gender: number // 0-未知 1-男 2-女
  score: number
  levelName: string
  status: number // 1-正常 0-禁用
  registerTime: string
  lastLoginTime: string
  remark: string
}

export const mockMembers: Member[] = [
  { id: 1, phone: '13800138001', nickname: '用户一', realName: '张三', idCard: '110101199001011234', gender: 1, score: 150, levelName: '黄金', status: 1, registerTime: '2024-01-15 10:20', lastLoginTime: '2024-09-01 08:30', remark: '优质用户' },
  { id: 2, phone: '13800138002', nickname: '用户二', realName: '李四', idCard: '310101199505056789', gender: 1, score: 80, levelName: '白银', status: 1, registerTime: '2024-02-20 14:30', lastLoginTime: '2024-08-28 16:00', remark: '' },
  { id: 3, phone: '13800138003', nickname: '用户三', realName: '王五', idCard: '440101199207103456', gender: 2, score: 520, levelName: '钻石', status: 0, registerTime: '2024-03-10 09:15', lastLoginTime: '2024-07-15 11:20', remark: '违规操作，已禁用' },
  { id: 4, phone: '13900139001', nickname: '大风吹', realName: '赵六', idCard: '510101198812124567', gender: 1, score: 10, levelName: '青铜', status: 1, registerTime: '2024-04-05 13:00', lastLoginTime: '2024-09-05 09:45', remark: '' },
  { id: 5, phone: '13900139002', nickname: '小李子', realName: '孙七', idCard: '320101199911309012', gender: 2, score: 300, levelName: '钻石', status: 1, registerTime: '2024-05-12 15:30', lastLoginTime: '2024-09-06 20:10', remark: '活跃用户' },
]

export interface Borrower {
  id: number
  memberId: number
  realName: string
  idCard: string
  phone: string
  gender: number
  auditStatus: number // 0-待审核 1-审核通过 2-已拒绝
  creditLimit: number // 授信额度
  usedLimit: number // 已用额度
  bankCard: string
  employer: string // 工作单位
  monthlyIncome: number // 月收入
  createTime: string
  auditTime: string
  remark: string
}

export const mockBorrowers: Borrower[] = [
  { id: 1, memberId: 1, realName: '张三', idCard: '110101199001011234', phone: '13800138001', gender: 1, auditStatus: 1, creditLimit: 100000, usedLimit: 20000, bankCard: '6222 02** **** 1234', employer: '腾讯科技', monthlyIncome: 25000, createTime: '2024-05-10 11:00', auditTime: '2024-05-12 09:30', remark: '信用良好' },
  { id: 2, memberId: 2, realName: '李四', idCard: '310101199505056789', phone: '13800138002', gender: 1, auditStatus: 2, creditLimit: 0, usedLimit: 0, bankCard: '6225 75** **** 5678', employer: '个体经营', monthlyIncome: 8000, createTime: '2024-06-15 16:20', auditTime: '2024-06-18 10:00', remark: '收入不稳定，已拒绝' },
  { id: 3, memberId: 4, realName: '赵六', idCard: '510101198812124567', phone: '13900139001', gender: 1, auditStatus: 0, creditLimit: 0, usedLimit: 0, bankCard: '6217 00** **** 9012', employer: '美团外卖', monthlyIncome: 12000, createTime: '2024-09-05 14:00', auditTime: '', remark: '待审核' },
]

export interface BorrowRecord {
  id: number
  borrowerName: string
  borrowerId: number
  amount: number
  term: number // 天数
  rate: number // 年化利率 %
  status: number // 0-待审核 1-审核通过 2-还款中 3-已结清 4-已拒绝
  purpose: string // 借款用途
  repayAmount: number // 已还金额
  applyTime: string
  auditTime: string
  repayEndTime: string // 应还清时间
  rejectReason: string // 拒绝原因
}

export const mockBorrowRecords: BorrowRecord[] = [
  { id: 1, borrowerName: '张三', borrowerId: 1, amount: 20000, term: 90, rate: 8.5, status: 2, purpose: '短期周转', repayAmount: 8000, applyTime: '2024-07-01 09:00', auditTime: '2024-07-02 14:00', repayEndTime: '2024-09-30 00:00', rejectReason: '' },
  { id: 2, borrowerName: '李四', borrowerId: 2, amount: 50000, term: 180, rate: 12.0, status: 1, purpose: '店铺装修', repayAmount: 0, applyTime: '2024-07-15 14:30', auditTime: '2024-07-16 10:00', repayEndTime: '2025-01-12 00:00', rejectReason: '' },
  { id: 3, borrowerName: '赵六', borrowerId: 3, amount: 10000, term: 30, rate: 10.0, status: 0, purpose: '日常消费', repayAmount: 0, applyTime: '2024-09-05 15:00', auditTime: '', repayEndTime: '', rejectReason: '' },
]

export interface LoanProject {
  id: number
  title: string
  borrowerName: string
  borrowerId: number
  amount: number
  rate: number
  term: number // 天数
  raisedAmount: number // 已募集金额
  progress: number // 募集进度 %
  status: number // 0-待发布 1-募资中 2-已完成 3-已逾期 4-已下架
  purpose: string // 借款用途
  riskLevel: number // 风险等级 1-低 2-中 3-高
  publishTime: string
  endTime: string // 募集截止时间
  createTime: string
  remark: string
}

export const mockLoanProjects: LoanProject[] = [
  { id: 1, title: '短期周转标A', borrowerName: '张三', borrowerId: 1, amount: 100000, rate: 8.5, term: 90, raisedAmount: 100000, progress: 100, status: 2, purpose: '短期周转', riskLevel: 1, publishTime: '2024-08-01 10:00', endTime: '2024-08-10 10:00', createTime: '2024-07-30 16:00', remark: '优质标的，按时还款' },
  { id: 2, title: '经营贷标B', borrowerName: '李四', borrowerId: 2, amount: 500000, rate: 12.0, term: 180, raisedAmount: 325000, progress: 65, status: 1, purpose: '店铺装修', riskLevel: 2, publishTime: '2024-08-15 15:30', endTime: '2024-08-25 15:30', createTime: '2024-08-14 09:00', remark: '' },
  { id: 3, title: '消费贷标C', borrowerName: '赵六', borrowerId: 3, amount: 10000, rate: 10.0, term: 30, raisedAmount: 0, progress: 0, status: 0, purpose: '日常消费', riskLevel: 3, publishTime: '', endTime: '', createTime: '2024-09-05 15:00', remark: '待发布，风险较高' },
  { id: 4, title: '教育贷标D', borrowerName: '孙七', borrowerId: 5, amount: 30000, rate: 9.0, term: 60, raisedAmount: 15000, progress: 50, status: 1, purpose: '学费贷款', riskLevel: 1, publishTime: '2024-09-01 10:00', endTime: '2024-09-11 10:00', createTime: '2024-08-31 14:00', remark: '' },
]

// ===== 标的投资记录 =====

export interface Investment {
  id: number
  projectId: number // 标的ID
  investorName: string // 投资人姓名
  investorId: number // 投资人ID
  amount: number // 投资金额
  status: number // 0-投资中 1-持有中 2-已退出 3-已收益
  investTime: string
  expectedReturn: number // 预期收益
  term: number // 投资期限（天）
}

export const mockInvestments: Investment[] = [
  { id: 1, projectId: 1, investorName: '王五', investorId: 10, amount: 50000, status: 3, investTime: '2024-08-01 12:00', expectedReturn: 1062, term: 90 },
  { id: 2, projectId: 1, investorName: '钱八', investorId: 11, amount: 30000, status: 3, investTime: '2024-08-02 09:30', expectedReturn: 637, term: 90 },
  { id: 3, projectId: 1, investorName: '周九', investorId: 12, amount: 20000, status: 3, investTime: '2024-08-03 14:00', expectedReturn: 425, term: 90 },
  { id: 4, projectId: 2, investorName: '王五', investorId: 10, amount: 150000, status: 1, investTime: '2024-08-15 16:00', expectedReturn: 7397, term: 180 },
  { id: 5, projectId: 2, investorName: '钱八', investorId: 11, amount: 100000, status: 1, investTime: '2024-08-16 10:00', expectedReturn: 4932, term: 180 },
  { id: 6, projectId: 2, investorName: '周九', investorId: 12, amount: 75000, status: 1, investTime: '2024-08-17 11:00', expectedReturn: 3699, term: 180 },
  { id: 7, projectId: 4, investorName: '王五', investorId: 10, amount: 8000, status: 1, investTime: '2024-09-01 11:00', expectedReturn: 118, term: 60 },
  { id: 8, projectId: 4, investorName: '钱八', investorId: 11, amount: 7000, status: 1, investTime: '2024-09-02 14:00', expectedReturn: 103, term: 60 },
]

// ===== RAG 知识库 =====

export interface KnowledgeBase {
  id: number
  name: string
  description: string
  docCount: number
  chunkCount: number
  embeddingModel: string
  status: number // 0-未启用 1-启用
  createTime: string
  updateTime: string
}

export const mockKnowledgeBases: KnowledgeBase[] = [
  { id: 1, name: '金融风控知识库', description: '包含风控规则、反欺诈策略等文档', docCount: 12, chunkCount: 348, embeddingModel: 'text-embedding-ada-002', status: 1, createTime: '2024-08-01 10:00', updateTime: '2024-09-05 14:20' },
  { id: 2, name: '产品手册库', description: '信贷产品说明、利率手册等', docCount: 5, chunkCount: 86, embeddingModel: 'text-embedding-ada-002', status: 1, createTime: '2024-08-15 09:00', updateTime: '2024-08-28 16:00' },
  { id: 3, name: '法规政策库', description: '金融法规、监管政策文件', docCount: 3, chunkCount: 52, embeddingModel: 'm3e-base', status: 0, createTime: '2024-09-01 11:00', updateTime: '2024-09-01 11:00' },
]

export interface KnowledgeDoc {
  id: number
  kbId: number // 知识库ID
  title: string
  source: string // 'pdf' | 'text' | 'url'
  content: string // 文本内容（text类型时存原文，pdf时为空）
  fileName: string // PDF 文件名
  fileSize: number // 文件大小 KB
  chunkCount: number
  status: number // 0-处理中 1-就绪 2-失败
  createTime: string
}

export const mockKnowledgeDocs: KnowledgeDoc[] = [
  { id: 1, kbId: 1, title: '反欺诈策略手册', source: 'pdf', content: '', fileName: 'anti-fraud.pdf', fileSize: 2048, chunkCount: 35, status: 1, createTime: '2024-08-01 10:30' },
  { id: 2, kbId: 1, title: '信用评分模型说明', source: 'text', content: '信用评分模型基于用户多维度数据进行综合评估，包括但不限于：还款历史、负债比、信用历史长度、新增信用查询、信用类型组合等维度。FICO评分范围300-850分，分数越高代表信用风险越低。', fileName: '', fileSize: 0, chunkCount: 12, status: 1, createTime: '2024-08-02 14:00' },
  { id: 3, kbId: 1, title: '风险等级划分标准', source: 'text', content: '风险等级分为低、中、高三级。低风险：信用评分>700，月收入>2万，负债比<30%。中风险：信用评分600-700，月收入1-2万，负债比30-50%。高风险：信用评分<600或负债比>50%。', fileName: '', fileSize: 0, chunkCount: 8, status: 1, createTime: '2024-08-05 09:15' },
  { id: 4, kbId: 2, title: '信贷产品利率表', source: 'pdf', content: '', fileName: 'rate-table.pdf', fileSize: 512, chunkCount: 6, status: 1, createTime: '2024-08-15 09:30' },
  { id: 5, kbId: 1, title: '催收流程规范', source: 'pdf', content: '', fileName: 'collection-flow.pdf', fileSize: 1536, chunkCount: 28, status: 0, createTime: '2024-09-05 14:20' },
]

// ===== 模型配置 =====

export interface ModelConfig {
  id: number
  name: string // 模型名称
  provider: string // 提供商: openai/anthropic/zhipu/qwen/baichuan/local
  modelType: string // 类型: chat/embedding/rerank
  modelName: string // 模型标识: gpt-4, text-embedding-ada-002 等
  apiUrl: string // API 地址
  apiKey: string // API Key（脱敏显示）
  maxTokens: number
  temperature: number
  isDefault: number // 1-默认模型 0-非默认
  status: number // 0-禁用 1-启用
  remark: string
  createTime: string
  updateTime: string
}

export const mockModelConfigs: ModelConfig[] = [
  { id: 1, name: 'GPT-4 对话模型', provider: 'openai', modelType: 'chat', modelName: 'gpt-4', apiUrl: 'https://api.openai.com/v1/chat/completions', apiKey: 'sk-****************************abcd', maxTokens: 8192, temperature: 0.7, isDefault: 1, status: 1, remark: '主力对话模型', createTime: '2024-08-01 10:00', updateTime: '2024-09-05 14:00' },
  { id: 2, name: 'Ada 嵌入模型', provider: 'openai', modelType: 'embedding', modelName: 'text-embedding-ada-002', apiUrl: 'https://api.openai.com/v1/embeddings', apiKey: 'sk-****************************abcd', maxTokens: 8191, temperature: 0, isDefault: 1, status: 1, remark: '知识库默认向量模型', createTime: '2024-08-01 10:30', updateTime: '2024-08-28 16:00' },
  { id: 3, name: 'GLM-4 对话模型', provider: 'zhipu', modelType: 'chat', modelName: 'glm-4', apiUrl: 'https://open.bigmodel.cn/api/paas/v4/chat/completions', apiKey: '****************************xyz', maxTokens: 4096, temperature: 0.7, isDefault: 0, status: 1, remark: '国产备选模型', createTime: '2024-08-15 09:00', updateTime: '2024-09-01 11:00' },
  { id: 4, name: 'M3E 嵌入模型', provider: 'local', modelType: 'embedding', modelName: 'm3e-base', apiUrl: 'http://localhost:8081/embed', apiKey: '', maxTokens: 512, temperature: 0, isDefault: 0, status: 0, remark: '本地部署向量模型', createTime: '2024-09-01 11:00', updateTime: '2024-09-01 11:00' },
  { id: 5, name: 'BGE Rerank 重排模型', provider: 'local', modelType: 'rerank', modelName: 'bge-reranker-large', apiUrl: 'http://localhost:8082/rerank', apiKey: '', maxTokens: 512, temperature: 0, isDefault: 1, status: 1, remark: '检索结果重排', createTime: '2024-09-02 14:00', updateTime: '2024-09-02 14:00' },
]

// ===== 智能客服咨询记录 =====

export interface ChatMessage {
  role: 'user' | 'assistant' // 用户提问 / AI回答
  content: string
  timestamp: string
}

export interface ChatSession {
  id: number
  userId: number
  userPhone: string
  userName: string
  title: string // 会话标题（取首条提问）
  messageCount: number // 消息条数
  model: string // 使用的模型
  kbName: string // 命中的知识库
  status: number // 0-进行中 1-已结束
  createTime: string
  updateTime: string
  messages: ChatMessage[] // 对话详情
}

export const mockChatSessions: ChatSession[] = [
  {
    id: 1, userId: 1, userPhone: '13800138001', userName: '张三',
    title: '借款利率是多少？',
    messageCount: 4, model: 'gpt-4', kbName: '金融风控知识库', status: 1,
    createTime: '2024-09-06 10:00', updateTime: '2024-09-06 10:05',
    messages: [
      { role: 'user', content: '借款利率是多少？', timestamp: '2024-09-06 10:00:12' },
      { role: 'assistant', content: '您好！我们的借款利率根据标的类型不同有所区别：\n\n1. 短期周转标：年化利率 8%-10%\n2. 经营贷标：年化利率 10%-12%\n3. 消费贷标：年化利率 9%-11%\n\n具体利率会根据您的信用等级和借款期限进行浮动。青铜等级最低利率9%起，钻石等级可享8%优惠利率。请问您想了解哪种产品的详情？', timestamp: '2024-09-06 10:00:18' },
      { role: 'user', content: '我的信用等级是什么？', timestamp: '2024-09-06 10:02:30' },
      { role: 'assistant', content: '根据您的账户信息，您当前的信用等级为「黄金」，积分 150 分。您可以借款的最高额度为 ¥100,000。如需提升等级，建议保持良好的还款记录并积累更多积分。', timestamp: '2024-09-06 10:02:35' },
    ],
  },
  {
    id: 2, userId: 2, userPhone: '13800138002', userName: '李四',
    title: '如何提高借款额度？',
    messageCount: 2, model: 'gpt-4', kbName: '产品手册库', status: 1,
    createTime: '2024-09-06 14:00', updateTime: '2024-09-06 14:03',
    messages: [
      { role: 'user', content: '如何提高借款额度？', timestamp: '2024-09-06 14:00:05' },
      { role: 'assistant', content: '提高借款额度有以下几种方式：\n\n1. **提升信用等级**：通过按时还款、参与平台活动等方式积累积分，达到更高等级即可提升额度上限。\n2. **完善个人信息**：补充工作单位、收入证明等资料，通过审核后可获得更高授信。\n3. **提供担保**：提供房产、车辆等资产证明，可申请担保贷款，额度更高。\n4. **保持良好还款记录**：连续按时还款6个月以上，系统会自动评估提升额度。\n\n当前您的等级为「白银」，最高可借 ¥50,000。建议先提升至黄金等级，额度可达 ¥100,000。', timestamp: '2024-09-06 14:00:20' },
    ],
  },
  {
    id: 3, userId: 4, userPhone: '13900139001', userName: '赵六',
    title: '审核需要多长时间？',
    messageCount: 2, model: 'glm-4', kbName: '产品手册库', status: 0,
    createTime: '2024-09-08 09:00', updateTime: '2024-09-08 09:02',
    messages: [
      { role: 'user', content: '审核需要多长时间？', timestamp: '2024-09-08 09:00:10' },
      { role: 'assistant', content: '您好！审核时间取决于您申请的类型：\n\n- **借款人认证**：通常1-2个工作日内完成审核\n- **借款申请**：审核通过后1个工作日内放款\n- **标的发布**：系统自动审核，实时发布\n\n您可以在「我的-审核进度」页面查看当前审核状态。如审核超时，请联系客服 400-xxx-xxxx。', timestamp: '2024-09-08 09:00:25' },
    ],
  },
  {
    id: 4, userId: 5, userPhone: '13900139002', userName: '孙七',
    title: '提前还款有手续费吗？',
    messageCount: 4, model: 'gpt-4', kbName: '金融风控知识库', status: 1,
    createTime: '2024-09-07 16:00', updateTime: '2024-09-07 16:10',
    messages: [
      { role: 'user', content: '提前还款有手续费吗？', timestamp: '2024-09-07 16:00:08' },
      { role: 'assistant', content: '您好！关于提前还款：\n\n- 借款期限30天以内的标的：支持提前还款，无手续费\n- 借款期限31-90天的标的：提前还款收取剩余利息的5%作为手续费\n- 借款期限90天以上的标的：提前还款收取剩余利息的10%作为手续费\n\n提前还款不影响您的信用评分。建议在还款日前操作。', timestamp: '2024-09-07 16:00:22' },
      { role: 'user', content: '那逾期了怎么办？', timestamp: '2024-09-07 16:05:00' },
      { role: 'assistant', content: '如果发生逾期，请尽快处理：\n\n1. **逾期1-3天**：系统发送提醒通知，不影响信用\n2. **逾期4-15天**：扣除信用积分，每日罚息为借款金额的0.05%\n3. **逾期15天以上**：降低信用等级，冻结借款功能，启动催收流程\n\n建议您设置自动还款避免逾期。如遇特殊情况，可申请延期还款（最多7天）。', timestamp: '2024-09-07 16:05:15' },
    ],
  },
  {
    id: 5, userId: 1, userPhone: '13800138001', userName: '张三',
    title: '怎么查看我的还款计划？',
    messageCount: 2, model: 'gpt-4', kbName: '产品手册库', status: 0,
    createTime: '2024-09-08 11:00', updateTime: '2024-09-08 11:01',
    messages: [
      { role: 'user', content: '怎么查看我的还款计划？', timestamp: '2024-09-08 11:00:05' },
      { role: 'assistant', content: '查看还款计划的步骤：\n\n1. 打开APP首页，点击底部「我的」\n2. 选择「我的借款」\n3. 点击对应借款标的，进入详情页\n4. 切换到「还款计划」标签页\n\n还款计划会显示每期的还款日期、还款金额（本金+利息）和状态。您也可以开启「自动还款」功能，到期日自动扣款。', timestamp: '2024-09-08 11:00:20' },
    ],
  },
]

// ===== Prompt 指令配置 =====

export interface PromptConfig {
  id: number
  promptKey: string // 指令标识（程序引用用），如 CS_SYSTEM_PROMPT
  name: string // 指令名称（展示用）
  scene: string // 使用场景: chat/audit/risk/summary
  contentType: string // 内容类型: prompt-系统指令 / keywords-关键词列表
  systemPrompt: string // 系统指令 / 关键词列表（每行一个）
  temperature: number
  maxTokens: number
  topP: number
  model: string // 绑定的模型
  isDefault: number // 1-默认 0-非默认
  status: number // 0-禁用 1-启用
  remark: string
  updateTime: string
}

export const mockPromptConfigs: PromptConfig[] = [
  {
    id: 1,
    promptKey: 'CS_SYSTEM_PROMPT',
    name: '客服系统指令',
    scene: 'chat',
    contentType: 'prompt',
    systemPrompt: '你是一个专业的金融客服助手，服务于「旺旺信贷」平台。\n\n你的职责：\n1. 解答用户关于借款、还款、利率、信用等级等问题\n2. 提供产品咨询和操作指引\n3. 引导用户完成相关业务流程\n\n注意事项：\n- 回答要简洁、准确、友好\n- 涉及具体利率和额度时，优先从知识库检索最新信息\n- 不要承诺未经审核批准的借款\n- 如遇到超出业务范围的问题，引导用户联系人工客服 400-xxx-xxxx\n\n回答格式：\n- 使用中文\n- 适当使用列表和加粗\n- 金额使用 ¥ 符号',
    temperature: 0.7,
    maxTokens: 2048,
    topP: 0.9,
    model: 'gpt-4',
    isDefault: 1,
    status: 1,
    remark: '客服对话主指令，控制回复风格和范围',
    updateTime: '2024-09-05 14:00',
  },
  {
    id: 2,
    promptKey: 'INTENT_SYSTEM_PROMPT',
    name: '意图识别指令',
    scene: 'chat',
    contentType: 'prompt',
    systemPrompt: '你是一个意图识别助手，负责分析用户输入的意图类别。\n\n意图类别：\n1. borrow_consult - 借款咨询（利率、额度、期限等）\n2. repay_consult - 还款咨询（还款计划、提前还款、逾期处理）\n3. account_consult - 账户咨询（信用等级、积分、个人信息）\n4. product_consult - 产品咨询（标的、产品介绍）\n5. complaint - 投诉建议\n6. chitchat - 闲聊问候\n7. handoff - 请求人工客服\n8. other - 其他\n\n输出格式（JSON）：\n{\n  "intent": "意图类别",\n  "confidence": 0.95,\n  "entities": [提取的关键实体],\n  "reply_hint": "回复建议"\n}\n\n注意：confidence < 0.7 时归为 other。',
    temperature: 0.2,
    maxTokens: 512,
    topP: 0.8,
    model: 'gpt-4',
    isDefault: 0,
    status: 1,
    remark: '用于识别用户意图，路由到不同处理流程',
    updateTime: '2024-09-05 15:00',
  },
  {
    id: 3,
    promptKey: 'UNSAFE_KEYWORDS',
    name: '不安全关键词',
    scene: 'risk',
    contentType: 'keywords',
    systemPrompt: '赌博\n色情\n暴力\n传销\n诈骗\n洗钱\n高利贷\n套现\n黑客\n刷单\n私下交易\n场外配资\n非法集资\n庞氏骗局',
    temperature: 0,
    maxTokens: 0,
    topP: 0,
    model: '-',
    isDefault: 1,
    status: 1,
    remark: '命中后拦截回复并触发风控告警，每行一个关键词',
    updateTime: '2024-09-06 09:00',
  },
  {
    id: 4,
    promptKey: 'HANDOFF_KEYWORDS',
    name: '转人工关键词',
    scene: 'chat',
    contentType: 'keywords',
    systemPrompt: '转人工\n人工客服\n找人工\n真人客服\n找客服\n转接人工\n人工服务\n客服人员\n联系客服\n找工作人员\n找人工客服\n接人工\n要人工\n不想跟机器人聊\n跟机器人聊',
    temperature: 0,
    maxTokens: 0,
    topP: 0,
    model: '-',
    isDefault: 1,
    status: 1,
    remark: '命中后自动转接人工客服，每行一个关键词',
    updateTime: '2024-09-06 09:30',
  },
  {
    id: 5,
    promptKey: 'csChitchatClient',
    name: '闲聊客户端指令',
    scene: 'chat',
    contentType: 'prompt',
    systemPrompt: '你是一个友好的闲聊助手，负责处理用户的日常寒暄和闲聊。\n\n适用场景：\n- 问候（你好、早上好）\n- 感谢（谢谢、辛苦了）\n- 天气闲聊\n- 情感表达\n- 笑话/趣味对话\n\n注意事项：\n- 保持轻松友好的语调\n- 回复简短自然，不要长篇大论\n- 不要主动引导到金融业务话题\n- 如果用户转向业务问题，提示「您可以详细描述您的问题，我会为您转接专业客服」\n- 涉及金融专业问题时不要随意回答，引导至专业客服',
    temperature: 0.9,
    maxTokens: 256,
    topP: 0.95,
    model: 'gpt-4',
    isDefault: 0,
    status: 1,
    remark: '处理非业务闲聊，控制闲聊边界',
    updateTime: '2024-09-04 11:00',
  },
  {
    id: 6,
    promptKey: 'RISK_SYSTEM_PROMPT',
    name: '风控审核指令',
    scene: 'risk',
    contentType: 'prompt',
    systemPrompt: '你是一个金融风控分析助手。\n\n你的职责：\n1. 根据用户的基本信息（信用评分、收入、负债比等）评估风险等级\n2. 给出风险提示和建议\n3. 识别潜在欺诈行为\n\n风险等级标准：\n- 低风险：信用评分>700，月收入>2万，负债比<30%\n- 中风险：信用评分600-700，月收入1-2万，负债比30-50%\n- 高风险：信用评分<600，负债比>50%\n\n输出格式：\n风险等级：[低/中/高]\n评估依据：[逐条列出]\n建议：[审批建议]',
    temperature: 0.3,
    maxTokens: 1024,
    topP: 0.85,
    model: 'gpt-4',
    isDefault: 0,
    status: 1,
    remark: '用于自动风控审核',
    updateTime: '2024-09-03 10:00',
  },
  {
    id: 7,
    promptKey: 'AUDIT_SYSTEM_PROMPT',
    name: '借款审核指令',
    scene: 'audit',
    contentType: 'prompt',
    systemPrompt: '你是一个借款审核助手，负责初步审核借款申请。\n\n审核维度：\n1. 借款人信用等级是否达标（青铜以上）\n2. 借款金额是否在授信额度内\n3. 借款用途是否合理合规\n4. 期限与还款能力是否匹配\n\n审核结论：\n- 通过：各项指标达标\n- 需人工复核：存在不确定因素\n- 拒绝：明显不符合条件\n\n输出格式：\n审核结论：[通过/需复核/拒绝]\n分析：[逐条分析各维度]\n建议额度：[建议的借款额度]',
    temperature: 0.2,
    maxTokens: 1024,
    topP: 0.8,
    model: 'glm-4',
    isDefault: 0,
    status: 1,
    remark: '借款申请自动初筛',
    updateTime: '2024-09-01 16:00',
  },
  {
    id: 8,
    promptKey: 'SUMMARY_SYSTEM_PROMPT',
    name: '对话摘要指令',
    scene: 'summary',
    contentType: 'prompt',
    systemPrompt: '你是一个对话摘要助手，负责将客服对话记录提炼为简洁摘要。\n\n要求：\n1. 提取用户的核心问题和诉求\n2. 总结客服给出的解决方案\n3. 标注是否需要后续跟进\n4. 摘要不超过200字\n\n输出格式：\n【用户诉求】...\n【解决方案】...\n【跟进事项】...',
    temperature: 0.5,
    maxTokens: 512,
    topP: 0.9,
    model: 'gpt-4',
    isDefault: 0,
    status: 0,
    remark: '会话结束后自动生成摘要',
    updateTime: '2024-08-28 09:00',
  },
]

// ===== 智能客服对话模型配置 =====
// 场景 → 绑定的模型 + Prompt 指令 + 知识库

export interface ChatModelBinding {
  id: number
  scene: string // 场景: intent/qa/chitchat/summary/safety
  sceneName: string // 场景名称
  modelId: number // 绑定的模型 ID（对应 ModelConfig.id）
  modelName: string // 模型名称（冗余，方便展示）
  promptId: number // 绑定的 Prompt 指令 ID
  promptKey: string // 指令标识
  kbId: number | null // 绑定的知识库 ID（null 表示不检索）
  kbName: string // 知识库名称
  enabled: number // 1-启用 0-禁用
  remark: string
  updateTime: string
}

export const mockChatModelBindings: ChatModelBinding[] = [
  {
    id: 1, scene: 'intent', sceneName: '意图识别',
    modelId: 1, modelName: 'GPT-4 对话模型',
    promptId: 2, promptKey: 'INTENT_SYSTEM_PROMPT',
    kbId: null, kbName: '—',
    enabled: 1, remark: '用户消息进来后先做意图识别',
    updateTime: '2024-09-06 10:00',
  },
  {
    id: 2, scene: 'qa', sceneName: '业务问答',
    modelId: 1, modelName: 'GPT-4 对话模型',
    promptId: 1, promptKey: 'CS_SYSTEM_PROMPT',
    kbId: 1, kbName: '金融风控知识库',
    enabled: 1, remark: '基于知识库检索回答业务问题',
    updateTime: '2024-09-06 10:05',
  },
  {
    id: 3, scene: 'chitchat', sceneName: '闲聊',
    modelId: 1, modelName: 'GPT-4 对话模型',
    promptId: 5, promptKey: 'csChitchatClient',
    kbId: null, kbName: '—',
    enabled: 1, remark: '处理非业务闲聊',
    updateTime: '2024-09-06 10:10',
  },
  {
    id: 4, scene: 'summary', sceneName: '对话摘要',
    modelId: 1, modelName: 'GPT-4 对话模型',
    promptId: 8, promptKey: 'SUMMARY_SYSTEM_PROMPT',
    kbId: null, kbName: '—',
    enabled: 0, remark: '会话结束后生成摘要',
    updateTime: '2024-09-06 10:15',
  },
  {
    id: 5, scene: 'safety', sceneName: '安全检测',
    modelId: 1, modelName: 'GPT-4 对话模型',
    promptId: 3, promptKey: 'UNSAFE_KEYWORDS',
    kbId: null, kbName: '—',
    enabled: 1, remark: '关键词命中拦截，不调模型',
    updateTime: '2024-09-06 10:20',
  },
]
