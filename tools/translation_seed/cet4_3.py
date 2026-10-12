# -*- coding: utf-8 -*-
"""四级真题句子翻译补充题（第 3 批）"""

STAGE = 1

ITEMS = [
    ("consist", "这本教材由十二个单元组成，每个单元都配有练习。",
     ["This textbook consists of twelve units, each with its own exercises.",
      "This textbook is consisted of twelve units, each with its own exercises.",
      "The textbook is made up of twelve units, every one of which has exercises.",
      "This textbook consists in twelve units, each with its own exercises."],
     "0,2",
     "consist of 与 be made up of 都表示「由……组成」；consist 没有被动语态（is consisted of 错误）。"
     "consist in 意为「在于」，语义不同。"),

    ("constant", "学习语言需要长期不断的练习。",
     ["Learning a language calls for constant practice over a long period.",
      "Learning a language calls for constant practise over a long period.",
      "It takes continuous practice over a long time to master a language.",
      "Learning a language calls for constantly practice over a long period."],
     "0,2",
     "practice 是名词，practise 在英式中作动词（美式一律用 practice）；"
     "修饰名词要用形容词 constant，不是副词 constantly。"),

    ("construct", "这座图书馆是用当地的一种青石建造的。",
     ["The library was constructed of a kind of local bluestone.",
      "The library was constructed by a kind of local bluestone.",
      "A type of local bluestone was used to build the library.",
      "The library constructed of a kind of local bluestone."],
     "0,2",
     "be constructed of 表示「用……建成」（看得出材料）；「图书馆被建造」需用被动语态，"
     "第四句缺谓语动词。by 引出的是动作执行者，不是材料。"),

    ("consume", "这台旧空调比新款的耗电多得多。",
     ["This old air conditioner consumes far more electricity than the new model.",
      "This old air conditioner consumes far more electricity as the new model.",
      "The old air conditioner uses much more power than the newer one.",
      "This old air conditioner consumes far more electric than the new model."],
     "0,2",
     "比较级 more ... than 是固定结构，不能把 than 换成 as；「电」是不可数名词 electricity，"
     "electric 是形容词「电动的」。"),

    ("contain", "这种水果富含维生素 C，对身体十分有益。",
     ["This fruit contains plenty of vitamin C and is very good for health.",
      "This fruit contains plenty of vitamin C, that is very good for health.",
      "Rich in vitamin C, this fruit is highly beneficial to the body.",
      "This fruit contains plenty of vitamin C and is very good to health."],
     "0,2",
     "contain / be rich in 都表示「富含」；非限制性定语从句不能用 that 引导，要用 which。"
     "be good for 表示「对……有益」，不用 to。"),

    ("convince", "他用自己的经历说服了同学们坚持晨读。",
     ["He convinced his classmates to keep reading aloud in the morning with his own experience.",
      "He convinced his classmates keeping reading aloud in the morning with his own experience.",
      "By sharing his own experience, he persuaded his classmates to stick to morning reading.",
      "He convinced that his classmates to keep reading aloud in the morning."],
     "0,2",
     "convince sb. to do sth. 表示「说服某人做某事」；persuade sb. to do 是同义表达。"
     "convince 不接动名词，也不说 convince that sb. to do。"),

    ("cooperate", "两个班级将合作完成这项社会调查。",
     ["The two classes will cooperate in completing the social survey.",
      "The two classes will cooperate to completing the social survey.",
      "The social survey will be carried out jointly by the two classes.",
      "The two classes will cooperate with completing the social survey."],
     "0,2",
     "cooperate in doing sth. 表示「合作做某事」；也可用 do sth. jointly。"
     "cooperate with 后接合作的对象（人），不接动名词。"),

    ("cope", "他不知道该如何应对如此繁重的课业。",
     ["He did not know how to cope with such heavy schoolwork.",
      "He did not know how to cope such heavy schoolwork.",
      "He had no idea how to deal with schoolwork that was so demanding.",
      "He did not know how to cope to such heavy schoolwork."],
     "0,2",
     "cope with 与 deal with 都表示「应对、处理」，介词不能省略或替换成 to。"),

    ("creative", "老师鼓励我们用更富创意的方式做笔记。",
     ["The teacher encouraged us to take notes in a more creative way.",
      "The teacher encouraged us to take notes in a more created way.",
      "We were encouraged by our teacher to note things down more imaginatively.",
      "The teacher encouraged us taking notes in a more creative way."],
     "0,2",
     "creative 是形容词「有创意的」；created 意为「被创造出来的」，不合语境。"
     "encourage sb. to do 不接动名词。"),

    ("critical", "批判性思维是大学阶段最重要的能力之一。",
     ["Critical thinking is one of the most important abilities at university.",
      "Critical thinking is one of the most important ability at university.",
      "Among the most important skills in college is the ability to think critically.",
      "Critic thinking is one of the most important abilities at university."],
     "0,2",
     "one of 后接复数名词 abilities；「批判性思维」固定译作 critical thinking，"
     "critic 是名词「评论家」，不能作定语。"),

    ("curious", "孩子们对一切新事物都充满好奇。",
     ["Children are curious about everything new.",
      "Children are curious with everything new.",
      "Everything new arouses children's curiosity.",
      "Children are curious for everything new."],
     "0,2",
     "be curious about 是固定搭配；也可用 arouse one's curiosity 表示「激起好奇心」。"
     "curious 后不接 with / for。"),

    ("deliver", "快递员每天要送两百多个包裹。",
     ["The courier has to deliver more than two hundred parcels every day.",
      "The courier has to deliver more than two hundred parcels every day to.",
      "Over two hundred packages are delivered by the courier each day.",
      "The courier has to delivery more than two hundred parcels every day."],
     "0,2",
     "deliver 是动词「递送」，delivery 是名词；has to 后接动词原形 deliver。"
     "句尾多余的 to 使句子不完整。"),

    ("demonstrate", "实验结果清楚地表明这种方法更有效。",
     ["The experimental results clearly demonstrate that this method is more effective.",
      "The experimental results clearly demonstrate this method more effective.",
      "The results of the experiment show clearly that this approach works better.",
      "The experimental results clearly demonstrate to that this method is more effective."],
     "0,2",
     "demonstrate / show that 后接完整从句；不能写成「demonstrate + 宾语 + 形容词」。"
     "demonstrate 是及物动词，后接从句时不加 to。"),

    ("deny", "他否认自己曾在考试中作弊。",
     ["He denied that he had ever cheated in the exam.",
      "He denied to cheat in the exam.",
      "He denied having cheated in any examination.",
      "He denied of cheating in the exam."],
     "0,2",
     "deny 后接 that 从句或动名词（deny doing / having done），不接不定式，也不加介词 of。"),

    ("depend", "能否按时完成，取决于我们能否合理分工。",
     ["Whether we can finish on time depends on whether we divide the work properly.",
      "Whether we can finish on time depends whether we divide the work properly.",
      "Finishing on time is dependent on a sensible division of labour among us.",
      "Whether we can finish on time depends on if we divide the work properly."],
     "0,2",
     "depend on 是固定搭配，介词 on 不可省；也可用 be dependent on。"
     "句首主语从句已经用了 whether，为避免重复，whether 比 if 更适合（if 不能引导主语从句）。"),

    ("derive", "很多英语单词都源自拉丁语和希腊语。",
     ["Many English words derive from Latin and Greek.",
      "Many English words derive of Latin and Greek.",
      "A large number of English words are derived from Latin and Greek.",
      "Many English words derive Latin and Greek."],
     "0,2",
     "derive from / be derived from 表示「源自」；derive 作不及物动词必须带 from，"
     "作及物动词时 derive A from B 意为「从 B 中提取 A」。"),

    ("deserve", "那些默默付出的志愿者理应得到我们的感谢。",
     ["Those volunteers who work quietly behind the scenes deserve our thanks.",
      "Those volunteers who work quietly behind the scenes deserve to our thanks.",
      "Our gratitude is well deserved by the volunteers who give so much without being noticed.",
      "Those volunteers who work quietly behind the scenes deserve being thanked."],
     "0,2",
     "deserve 是及物动词，直接接名词（deserve our thanks）；deserve to be thanked 才对，"
     "deserve being done 不成立。"),

    ("determine", "一个人的习惯往往决定了他的未来。",
     ["A person's habits often determine his future.",
      "A person's habits often determine to his future.",
      "It is often one's habits that shape one's future.",
      "A person's habits often determined his future."],
     "0,2",
     "determine 是及物动词，直接接宾语；shape 可作同义替换。"
     "句中有 often，表经常性情况，用一般现在时 determine。"),

    ("devote", "她把大部分业余时间都用在了阅读英文小说上。",
     ["She devotes most of her spare time to reading English novels.",
      "She devotes most of her spare time to read English novels.",
      "Most of her free time is devoted to English fiction reading.",
      "She devotes most of her spare time for reading English novels."],
     "0,2",
     "devote ... to ... 中 to 是介词，后接动名词 reading；不能换成 for，也不能接动词原形。"),

    ("distinguish", "小孩子很难分辨这两种鸟的叫声。",
     ["It is hard for small children to distinguish between the calls of these two birds.",
      "It is hard for small children to distinguish the calls of these two birds between.",
      "Little children can hardly tell the two birds' calls apart.",
      "It is hard for small children to distinguish among the calls of these two birds."],
     "0,2",
     "distinguish between A and B 表示「区分两者」；也可说 tell A and B apart。"
     "两者之间用 between，三者以上才用 among。"),
]
