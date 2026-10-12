# -*- coding: utf-8 -*-
"""四级真题句子翻译补充题（第 2 批）"""

STAGE = 1

ITEMS = [
    ("burden", "沉重的作业负担正在影响孩子们的睡眠。",
     ["Heavy homework burdens are affecting children's sleep.",
      "Heavy homework burden is affecting children's sleep.",
      "Children's sleep is being affected by the heavy load of homework.",
      "Heavy homework burdens are effecting children's sleep."],
     "0,2",
     "burden 作可数名词，谓语 are 对应复数 burdens；也可用 the heavy load of homework。"
     "effect 作动词意为「实现、促成」，「影响」的动词是 affect。"),

    ("cancel", "由于暴雨，运动会不得不被取消。",
     ["The sports meet had to be cancelled because of the heavy rain.",
      "The sports meet had to be canceled because of the heavy rain.",
      "Owing to the heavy rain, the sports meeting had to be called off.",
      "The sports meet had to cancel because of the heavy rain."],
     "0,1",
     "cancel 的过去分词英式拼 cancelled、美式 canceled，两种都正确；call off 是同义短语动词。"
     "「运动会被取消」要用被动 be cancelled，主动语态 cancel 变成「运动会被迫取消（某物）」，逻辑错误。"),

    ("challenge", "如何在短时间内记住大量单词，对我们是个挑战。",
     ["How to memorise a large number of words in a short time is a challenge for us.",
      "How to memorise a large number of words in a short time is a challenge of us.",
      "It is a challenge for us to memorise large quantities of words within a short period.",
      "How to memorise a large number of words in a short time is a challenge for we."],
     "0,2",
     "a challenge for sb. 表示「对某人而言是挑战」，介词用 for 不用 of；for 后接宾格 us，不能用主格 we。"),

    ("classify", "这些书可以按照主题分为三类。",
     ["These books can be classified into three categories by theme.",
      "These books can be classified as three categories by theme.",
      "These books may be divided into three groups according to their subject.",
      "These books can be classified in three categories by theme."],
     "0,2",
     "classify ... into ... 表示「把……分成……类」；classify ... as ... 表示「把……认定为……」，语义不同。"
     "搭配介词不能用 in。"),

    ("combine", "这门课把理论学习与实践训练结合起来。",
     ["This course combines theoretical study with practical training.",
      "This course combines theoretical study to practical training.",
      "Theoretical study and practical training are combined in this course.",
      "This course combines theoretical study and practical training together."],
     "0,2",
     "combine A with B 是固定搭配；也可用 A and B are combined in ...。"
     "combine 本身已含「合在一起」之意，再加 together 语义重复。"),

    ("comfort", "这本旧相册总能在我低落时给我安慰。",
     ["This old photo album never fails to comfort me when I feel down.",
      "This old photo album never fails to comfort to me when I feel down.",
      "This old photo album always brings me comfort whenever I am feeling low.",
      "This old photo album never fails to comfortable me when I feel down."],
     "0,2",
     "comfort 作动词是及物的，直接接宾语；也可用 bring sb. comfort 的名词用法。"
     "comfortable 是形容词，不能作谓语。"),

    ("comment", "教授对我们的实验报告提出了几点意见。",
     ["The professor made several comments on our experiment report.",
      "The professor made several comments to our experiment report.",
      "The professor commented on our lab report at several points.",
      "The professor made several comment on our experiment report."],
     "0,2",
     "comment on sth. / make comments on sth. 介词固定用 on；several 后接复数 comments。"),

    ("compare", "与去年相比，今年参加长跑的人数翻了一倍。",
     ["Compared with last year, the number of long-distance runners has doubled this year.",
      "Comparing with last year, the number of long-distance runners has doubled this year.",
      "The number of runners in the long-distance race is twice what it was last year.",
      "Compared with last year, the number of long-distance runners has doubled twice this year."],
     "0,2",
     "「与……相比」作状语时，主语是被比较的对象，要用过去分词 Compared with；"
     "Comparing 是主动含义，会让「人数」去执行比较动作。doubled 已含「翻倍」，再加 twice 重复。"),

    ("compensate", "勤能补拙，这是老师常对我们说的一句话。",
     ["Diligence can compensate for a lack of natural talent, as our teacher often tells us.",
      "Diligence can compensate a lack of natural talent, as our teacher often tells us.",
      "Hard work can make up for a lack of cleverness, which is what our teacher often says.",
      "Diligence can compensate to a lack of natural talent, as our teacher often tells us."],
     "0,2",
     "compensate for 表示「弥补」，介词 for 必须保留；make up for 是同义替换。"
     "compensate 直接接宾语时意为「赔偿某人」，语义不符。"),

    ("complain", "不少学生抱怨食堂的菜品种类太少。",
     ["Quite a few students complain about the limited variety of dishes in the canteen.",
      "Quite a few students complain the limited variety of dishes in the canteen.",
      "Many students complain that there are too few choices of dishes in the dining hall.",
      "Quite a few students complain for the limited variety of dishes in the canteen."],
     "0,2",
     "complain about sth. 或 complain that 从句；complain 是不及物动词，不能直接接宾语，也不能用 complain for。"),

    ("complex", "这是一个涉及经济、环境与社会的复杂问题。",
     ["This is a complex problem involving the economy, the environment and society.",
      "This is a complex problem involved the economy, the environment and society.",
      "The problem is a complicated one that touches on economy, environment and society.",
      "This is a complex problem that is involved the economy, the environment and society."],
     "0,2",
     "「涉及……」作定语，problem 与 involve 是主动关系，用现在分词 involving；"
     "用 involved 要写成 involved in，且此处语义不当。complex 与 complicated 可互换。"),

    ("concentrate", "安静的环境有助于我集中精力背单词。",
     ["A quiet environment helps me concentrate on memorising words.",
      "A quiet environment helps me concentrate in memorising words.",
      "It is easier for me to focus my attention on learning words in a quiet place.",
      "A quiet environment helps me concentrate to memorise words."],
     "0,2",
     "concentrate on (doing) sth. 是固定搭配，介词用 on；也可用 focus one's attention on。"
     "concentrate 后不接不定式。"),

    ("conclude", "研究小组得出结论：早睡的学生成绩更好。",
     ["The research team concluded that students who go to bed early perform better.",
      "The research team concluded to that students who go to bed early perform better.",
      "The team came to the conclusion that early sleepers tend to get better grades.",
      "The research team concluded students who go to bed early perform better that."],
     "0,2",
     "conclude 后直接接 that 从句，不加介词；名词用法是 come to / draw the conclusion that ...。"),

    ("conduct", "下周将在实验室进行一项关于记忆的实验。",
     ["An experiment on memory will be conducted in the lab next week.",
      "An experiment on memory will be conducted at the lab next week.",
      "The researchers will carry out a memory experiment in the laboratory next week.",
      "An experiment on memory will conduct in the lab next week."],
     "0,2",
     "conduct / carry out an experiment 表示「做实验」；实验是被做的，要用被动 be conducted。"
     "「在实验室」用 in the lab，at the lab 不地道。"),

    ("confident", "她对自己的英语口语越来越有信心。",
     ["She is becoming more and more confident about her spoken English.",
      "She is becoming more and more confident in her spoken English.",
      "Her confidence in her oral English is growing steadily.",
      "She is becoming more and more confident with her spoken English."],
     "0,2",
     "be confident about / of sth.；be confident in sb./oneself 也可，但指对人有信心；"
     "表示「对某事有把握」最稳妥是 about。confident with 不是规范搭配。"),

    ("confirm", "请尽快确认你是否能参加这次讲座。",
     ["Please confirm whether you can attend this lecture as soon as possible.",
      "Please confirm me whether you can attend this lecture as soon as possible.",
      "Please let us know at your earliest convenience if you are able to come to the lecture.",
      "Please confirm that if you can attend this lecture as soon as possible."],
     "0,2",
     "confirm 后接 whether / if 从句或名词，不接「人 + 从句」的双宾语结构；"
     "confirm that 与 whether 不能叠用。"),

    ("conflict", "他的观点与研究报告的结论相冲突。",
     ["His view conflicts with the conclusion of the research report.",
      "His view conflicts to the conclusion of the research report.",
      "His opinion is in conflict with what the research report concludes.",
      "His view is conflicted with the conclusion of the research report."],
     "0,2",
     "conflict with 是固定搭配；名词形式是 be in conflict with。"
     "「相冲突」不用被动式 is conflicted with。"),

    ("confuse", "这两个单词太像了，我总是把它们弄混。",
     ["These two words are so similar that I always confuse them.",
      "These two words are so similar that I always confuse with them.",
      "I can never tell these two words apart because they look so much alike.",
      "These two words are so similar that I always confused them."],
     "0,2",
     "confuse 是及物动词，直接接宾语；tell A and B apart 表示「把两者区分开」。"
     "句中有 always、描述当前习惯，用一般现在时 confuse，不是过去式 confused。"),

    ("connect", "这条新地铁线把城南与机场连接起来。",
     ["The new subway line connects the south of the city with the airport.",
      "The new subway line connects the south of the city and the airport.",
      "The south of the city is linked to the airport by the new metro line.",
      "The new subway line connects the south of the city to the airport with."],
     "0,2",
     "connect A with/to B 与 link A to B 均可；直接用 connect A and B 不地道（缺少介词）。"
     "句尾多一个 with 属结构残缺。"),

    ("consider", "学校正在考虑把体育课增加到每周三节。",
     ["The school is considering increasing PE classes to three a week.",
      "The school is considering to increase PE classes to three a week.",
      "The school is thinking about raising the number of PE lessons to three per week.",
      "The school is considered increasing PE classes to three a week."],
     "0,2",
     "consider 后接动名词（consider doing），不接不定式；也可用 think about doing。"
     "「学校正在考虑」是主动，不用被动 is considered。"),
]
