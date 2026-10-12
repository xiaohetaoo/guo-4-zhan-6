# -*- coding: utf-8 -*-
"""四级真题句子翻译补充题（第 5 批）"""

STAGE = 1

ITEMS = [
    ("feature", "新款手机的一大特点是电池续航更长。",
     ["A key feature of the new phone is its longer battery life.",
      "A key feature of the new phone is its longer battery living.",
      "The new model features a battery that lasts much longer.",
      "A key feature of the new phone is its longer battery life for."],
     "0,2",
     "feature 可作名词「特点」和动词「以……为特色」；「续航」用 battery life，"
     "living 指「生活、生计」，不能替换 life。"),

    ("finance", "他靠兼职来支付自己的大学学费。",
     ["He finances his college education by doing part-time jobs.",
      "He finances his college education by doing part-time jobs for.",
      "He pays his way through university with part-time work.",
      "He financial his college education by doing part-time jobs."],
     "0,2",
     "finance 可作动词「为……提供资金」；pay one's way through 表示「靠自己负担费用」。"
     "financial 是形容词，不能作谓语。"),

    ("flexible", "我们的复习计划应当灵活一些，以便随时调整。",
     ["Our revision plan should be flexible so that it can be adjusted at any time.",
      "Our revision plan should be flexible so that it can be adjusting at any time.",
      "We should keep our review schedule flexible enough to adapt to changes.",
      "Our revision plan should be flexibility so that it can be adjusted at any time."],
     "0,2",
     "flexible 是形容词，flexibility 是名词，be 动词后接形容词；"
     "「计划被调整」用被动 be adjusted，不是 be adjusting。"),

    ("focus", "他把注意力集中在如何提高口语上。",
     ["He focused his attention on how to improve his spoken English.",
      "He focused his attention in how to improve his spoken English.",
      "His attention was centred on the improvement of his oral English.",
      "He focused his attention to how to improve his spoken English."],
     "0,2",
     "focus / centre one's attention on (doing) sth. 介词固定用 on，不用 in 或 to。"),

    ("forbid", "图书馆内严禁大声喧哗。",
     ["Making loud noise is strictly forbidden in the library.",
      "Making loud noise is strictly forbidden to in the library.",
      "Library visitors are not allowed to talk loudly.",
      "Making loud noise is strictly forbidding in the library."],
     "0,2",
     "forbid 的过去分词是 forbidden（forbade 是过去式）；「被禁止」用被动 be forbidden，"
     "现在分词 forbidding 意为「令人生畏的」。"),

    ("formal", "参加毕业典礼时，学生应当穿着正式。",
     ["Students are expected to dress formally at the graduation ceremony.",
      "Students are expected to dress formal at the graduation ceremony.",
      "It is required that students wear formal dress for the commencement.",
      "Students are expected to dress formally at the graduation ceremony with."],
     "0,2",
     "修饰动词 dress 要用副词 formally，不能用形容词 formal；也可用 wear formal dress。"
     "名词形式 ceremony 前用 at，句尾不可再接孤立介词。"),

    ("frequent", "他和笔友之间的通信越来越频繁。",
     ["Their correspondence with each other has become more and more frequent.",
      "Their correspondence with each other has become more and more frequently.",
      "They write to each other with increasing frequency.",
      "Their correspondence with each other has become more frequent and more."],
     "0,2",
     "become 是系动词，后接形容词 frequent 作表语；也可用 with increasing frequency。"
     "比较级的正确重复形式是 more and more frequent。"),

    ("fundamental", "阅读是语言学习中最基本的技能之一。",
     ["Reading is one of the most fundamental skills in language learning.",
      "Reading is one of the most fundamental skill in language learning.",
      "Among the basic skills of language study, reading is essential.",
      "Reading is one of the most fundamentally skills in language learning."],
     "0,2",
     "one of 后接复数名词 skills；修饰名词用形容词 fundamental，"
     "fundamentally 是副词，不能作定语。"),

    ("generate", "这台设备每天能产生足够的电力供全村使用。",
     ["This equipment can generate enough electricity for the whole village every day.",
      "This equipment can generate enough electricity to the whole village every day.",
      "Enough power for the entire village is produced by this device each day.",
      "This equipment can generate enough electric for the whole village every day."],
     "0,2",
     "generate / produce electricity 表示「发电」；「供某人使用」用 for sb."
     "electric 是形容词，名词形式为 electricity。"),

    ("genuine", "他对中国书法表现出浓厚的兴趣。",
     ["He has shown a genuine interest in Chinese calligraphy.",
      "He has shown a genuine interest of Chinese calligraphy.",
      "His interest in Chinese calligraphy is truly sincere.",
      "He has shown a genuinely interest in Chinese calligraphy."],
     "0,2",
     "interest in sth. 介词用 in；修饰名词 interest 用形容词 genuine，"
     "genuinely 是副词，不能作定语。"),

    ("gradual", "他的英语水平在一年里有了稳步的提升。",
     ["His English has improved steadily over the year.",
      "His English has improved gradual over the year.",
      "There has been a steady improvement in his English during the past year.",
      "His English has improved gradually over the year for."],
     "0,2",
     "修饰动词 improved 要用副词 steadily / gradually，不能用形容词 gradual；"
     "也可用名词结构 a steady improvement in ...。"),

    ("grasp", "抓住每一次开口说英语的机会很重要。",
     ["It is important to grasp every chance to speak English.",
      "It is important to grasp every chance speaking English.",
      "Seizing every opportunity to practise oral English matters a great deal.",
      "It is important to grasp every chance for speak English."],
     "0,2",
     "grasp / seize the chance to do sth.；「机会」后接不定式作定语，"
     "不用 chance speaking，也不用 for speak。"),

    ("handle", "她很擅长处理突发的紧急情况。",
     ["She is good at handling unexpected emergencies.",
      "She is good at handle unexpected emergencies.",
      "She deals with sudden emergencies very well.",
      "She is good at handling unexpected emergency."],
     "0,2",
     "be good at 后接动名词 handling；handle / deal with 都表示「处理」。"
     "「各种紧急情况」表泛指类别，用复数 emergencies。"),

    ("harmony", "这幅画体现了人与自然的和谐共处。",
     ["This painting shows the harmonious coexistence of man and nature.",
      "This painting shows the harmony coexistence of man and nature.",
      "The picture expresses how humans and nature live in harmony.",
      "This painting shows the harmonious coexist of man and nature."],
     "0,2",
     "修饰名词 coexistence 要用形容词 harmonious；也可说 live in harmony（用名词）。"
     "coexist 是动词，不能作 shows 的宾语。"),

    ("harvest", "秋天是农民们收获庄稼的季节。",
     ["Autumn is the season when farmers harvest their crops.",
      "Autumn is the season when farmers harvest their crop.",
      "It is in autumn that the peasants bring in their harvest.",
      "Autumn is the season when farmers harvest for their crops."],
     "0,2",
     "harvest 可作动词（harvest crops）和名词（bring in the harvest）；"
     "harvest 是及物动词，后不加 for。crop 表泛指时用复数 crops。"),

    ("hesitate", "遇到不懂的问题时，请不要犹豫向老师请教。",
     ["When you meet a problem you do not understand, do not hesitate to ask your teacher.",
      "When you meet a problem you do not understand, do not hesitate asking your teacher.",
      "Never hesitate about turning to your teacher when something puzzles you.",
      "When you meet a problem you do not understand, do not hesitate of asking your teacher."],
     "0,2",
     "hesitate to do 与 hesitate about doing 都成立；但 hesitate 后不接动名词（无 about 时），"
     "也不与 of 搭配。"),

    ("proud", "能被选为学生代表，他感到非常自豪。",
     ["He was proud to be chosen as the student representative.",
      "He was pride to be chosen as the student representative.",
      "It was a great honour for him to be elected student representative.",
      "He was proud to be choosing as the student representative."],
     "0,2",
     "proud 是形容词，be proud to do 表示「为做某事而自豪」；pride 是名词（take pride in）。"
     "「被选为」要用被动 to be chosen，不是 to be choosing。"),

    ("ideal", "这个安静的小院是读书的理想场所。",
     ["This quiet courtyard is an ideal place for reading.",
      "This quiet courtyard is an ideal place of reading.",
      "Nothing could be better than this peaceful yard as a spot for study.",
      "This quiet courtyard is an ideally place for reading."],
     "0,2",
     "place for doing sth. 表示「做某事的场所」，介词用 for；"
     "修饰名词 place 用形容词 ideal，ideally 是副词。"),

    ("ignore", "我们不能忽视睡眠不足带来的危害。",
     ["We cannot ignore the harm caused by lack of sleep.",
      "We cannot ignore the harm causing by lack of sleep.",
      "The damage done by insufficient sleep cannot be overlooked.",
      "We cannot ignore the harm caused by lack of sleep to."],
     "0,2",
     "「由……造成的危害」用过去分词 caused by（表被动）；现在分词 causing 表主动，逻辑错误。"
     "ignore / overlook 均表示「忽视」。"),

    ("illustrate", "老师用一个生动的例子说明了这个语法点。",
     ["The teacher illustrated this grammar point with a vivid example.",
      "The teacher illustrated this grammar point by a vivid example.",
      "A lively example was used by the teacher to explain the grammar item.",
      "The teacher illustrated this grammar point with a vivid example for."],
     "0,2",
     "illustrate sth. with ... 表示「用……说明」；也可用 explain ... with ...。"
     "句尾孤立介词 for 使句子残缺。"),

    ("imitate", "孩子通过模仿大人来学习说话。",
     ["Children learn to speak by imitating adults.",
      "Children learn to speak by imitating to adults.",
      "By copying grown-ups, children pick up how to talk.",
      "Children learn to speak by imitate adults."],
     "0,2",
     "imitate / copy 是及物动词，直接接宾语；by 是介词，后接动名词 imitating，不是动词原形。"),

    ("imply", "他的沉默似乎暗示着不同意。",
     ["His silence seemed to imply that he did not agree.",
      "His silence seemed to imply him not agreeing.",
      "His silence appeared to suggest disagreement.",
      "His silence seemed to imply to that he did not agree."],
     "0,2",
     "imply / suggest 后接 that 从句或名词；不接「宾语 + 分词」结构，也不加介词 to。"),

    ("impose", "过多的作业会给学生带来沉重的压力。",
     ["Too much homework imposes heavy pressure on students.",
      "Too much homework imposes heavy pressure to students.",
      "Students are put under great stress by an excessive amount of homework.",
      "Too much homework imposes heavy pressure with students."],
     "0,2",
     "impose sth. on sb. 介词固定用 on；也可用 put sb. under stress。"),

    ("impress", "那位老教授的讲座给所有学生留下了深刻印象。",
     ["The old professor's lecture left a deep impression on all the students.",
      "The old professor's lecture left a deep impression to all the students.",
      "All the students were deeply impressed by the professor's talk.",
      "The old professor's lecture impressed on all the students deeply."],
     "0,2",
     "leave an impression on sb. 与 sb. be impressed by sth. 是标准搭配；"
     "impress on sb. 后需接具体内容（impress sth. on sb.）。"),

    ("incident", "旅途中的一件小事改变了他对这座城市的看法。",
     ["A small incident during the trip changed his view of the city.",
      "A small accident during the trip changed his view of the city.",
      "His opinion of the city was changed by a minor episode on the journey.",
      "A small incident during the trip changed his view of the city for."],
     "0,2",
     "incident 指「小事件、插曲」，accident 指「意外事故」，此处不是事故；"
     "change one's view of sth. 是固定搭配。"),
]
