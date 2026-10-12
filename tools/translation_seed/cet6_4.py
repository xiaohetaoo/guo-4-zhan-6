# -*- coding: utf-8 -*-
"""六级真题句子翻译补充题（第 4 批）"""

STAGE = 2

ITEMS = [
    ("feasible", "在山区推广在线教学是完全可行的。",
     ["Promoting online teaching in mountainous areas is entirely feasible.",
      "Promoting online teaching in mountainous areas is entirely feasible to.",
      "It is quite practicable to introduce online instruction in mountain regions.",
      "Promoting online teaching in mountainous areas is entirely feasibly."],
     "0,2",
     "feasible / practicable 是形容词「可行的」，作表语；feasibly 是副词，不能作表语。"
     "句尾不可再加孤立介词 to。"),

    ("fluctuate", "这两年该校的报考人数一直在波动。",
     ["The number of applicants to this school has been fluctuating over the past two years.",
      "The number of applicants to this school has been fluctuated over the past two years.",
      "Applications to the school have gone up and down during the last two years.",
      "The number of applicants to this school has been fluctuating over the past two years to."],
     "0,2",
     "fluctuate 作「波动」讲不用被动语态，has been fluctuating 是现在完成进行时；"
     "go up and down 是口语化同义表达。"),

    ("foster", "阅读经典有助于培养孩子的想象力。",
     ["Reading classics helps to foster children's imagination.",
      "Reading classics helps to foster children's imagination of.",
      "Children's imagination is nurtured by their reading of classic works.",
      "Reading classics helps fostering children's imagination."],
     "0,2",
     "help to do 与 help do 都成立，但 help doing 不成立；foster / nurture 表示「培养」。"),

    ("hamper", "资金短缺严重制约了这项研究的进展。",
     ["A shortage of funds has seriously hampered the progress of this research.",
      "A shortage of funds has seriously hampered the progress of this research to.",
      "The research has been badly held back by a lack of money.",
      "A shortage of funds has seriously hampered with the progress of this research."],
     "0,2",
     "hamper 是及物动词，直接接宾语；hold back 是同义短语动词，此处用被动 has been held back。"),

    ("harness", "人们正在学习更好地利用风能。",
     ["People are learning to harness wind energy more effectively.",
      "People are learning to harness wind energy more effective.",
      "Better use is being made of wind power than ever before.",
      "People are learning to harness wind energy more effectively with."],
     "0,2",
     "harness 表示「利用（自然力）」；修饰动词要用副词 effectively，不是形容词 effective。"
     "make use of 是名词化同义结构。"),

    ("hypothesis", "这一假设还需要更多的实验来检验。",
     ["This hypothesis still needs to be tested by more experiments.",
      "This hypothesis still needs being tested by more experiments.",
      "More experiments are required to put this supposition to the test.",
      "This hypothesis still needs to test by more experiments."],
     "0,2",
     "need to be done 表示「需要被……」；need doing（英式）也可，但 need being done 不成立。"
     "假设是被检验的对象，不能用主动不定式 to test。"),

    ("identical", "这两份问卷的结果几乎完全一致。",
     ["The results of the two questionnaires are almost identical.",
      "The results of the two questionnaires are almost identical with each other.",
      "There is hardly any difference between the findings of the two surveys.",
      "The results of the two questionnaires are almost identity."],
     "0,2",
     "identical 是形容词；be identical with/to 后须接比较对象，句中没有对象时不必加；"
     "identity 是名词「身份、同一性」。"),

    ("illuminate", "这些发现有助于阐明语言习得的机制。",
     ["These findings help to illuminate the mechanism of language acquisition.",
      "These findings help to illuminate the mechanism of language acquisition to.",
      "The mechanism by which a language is acquired is clarified by such findings.",
      "These findings help to illumination the mechanism of language acquisition."],
     "0,2",
     "illuminate / clarify 是动词「阐明」；illumination 是名词，不能接在 to 之后作谓语。"),

    ("impair", "长期睡眠不足会损害记忆力。",
     ["A long-term lack of sleep can impair memory.",
      "A long-term lack of sleep can impair memory of.",
      "Memory may be damaged by chronic sleep deprivation.",
      "A long-term lack of sleep can impair with memory."],
     "0,2",
     "impair / damage 是及物动词，直接接宾语；句尾不可再加介词。"
     "chronic sleep deprivation 表示「长期睡眠不足」。"),

    ("impart", "优秀的教师不仅传授知识，也传递做人的道理。",
     ["Good teachers impart not only knowledge but also principles of how to behave.",
      "Good teachers impart not only knowledge but also principles of how to behave to.",
      "What a fine teacher passes on is knowledge as well as the way to conduct oneself.",
      "Good teachers impart with not only knowledge but also principles of conduct."],
     "0,2",
     "impart / pass on 是及物动词（短语），直接接宾语，不加 with。"),

    ("restrict", "缺乏交流严重限制了团队的工作效率。",
     ["A lack of communication has seriously restricted the efficiency of the team.",
      "A lack of communication has seriously restricted the efficiency of the team to.",
      "Team efficiency suffers badly when members fail to communicate.",
      "A lack of communication has seriously restricted with the efficiency of the team."],
     "0,2",
     "restrict 是及物动词，直接接宾语；也可用 suffer / be affected 转换句式。"),

    ("imperative", "保护学生的视力是当务之急。",
     ["Protecting students' eyesight is an imperative task.",
      "Protecting students' eyesight is an imperatively task.",
      "It is imperative that steps be taken to protect the eyesight of students.",
      "Protecting students' eyesight is an imperative task of."],
     "0,2",
     "imperative 可作形容词「迫切的」；修饰名词 task 用形容词，imperatively 是副词。"
     "It is imperative that ... 后接虚拟语气（be taken / should be taken）。"),

    ("implement", "新方案将于下学期在全校推行。",
     ["The new scheme will be implemented across the school next semester.",
      "The new scheme will implement across the school next semester.",
      "Next term will see the new plan put into practice throughout the school.",
      "The new scheme will be implemented across the school next semester to."],
     "0,2",
     "方案是被推行的对象，要用被动 be implemented；put sth. into practice 是同义表达。"),

    ("implicit", "他的话里隐含着对这种做法的批评。",
     ["His words carried an implicit criticism of this practice.",
      "His words carried an implicit criticism to this practice.",
      "What he said implied a criticism of the way things were done.",
      "His words carried an implicate criticism of this practice."],
     "0,2",
     "implicit 是形容词「含蓄的、隐含的」；implicate 是动词「牵涉」，不能作定语。"
     "criticism of 表示「对……的批评」。"),

    ("incidence", "近年来青少年近视的发生率持续上升。",
     ["In recent years the incidence of short-sightedness among teenagers has kept rising.",
      "In recent years the incidence of short-sightedness among teenagers has kept raising.",
      "Short-sightedness has become increasingly common among adolescents of late.",
      "In recent years the incident of short-sightedness among teenagers has kept rising."],
     "0,2",
     "incidence 表示「发生率」，incident 表示「事件」；rise 是不及物动词「上升」，"
     "raise 是及物动词「提高」，主语是发生率时只能用 rise。"),

    ("inclusive", "这门课面向所有专业背景的学生开放。",
     ["This course is open to students of all academic backgrounds.",
      "This course is open to students of all academic backgrounds for.",
      "No matter what their major is, students are welcome to take the course.",
      "This course is inclusive to students of all academic backgrounds."],
     "0,2",
     "be open to sb. 表示「向某人开放」；inclusive 表示「包容的、包括一切的」，"
     "通常说 an inclusive course（包容性课程），不说 inclusive to sb.。"),

    ("incur", "延期提交会产生额外的费用。",
     ["Late submission will incur additional charges.",
      "Late submission will incur additional charges to.",
      "Extra fees will be charged if the work is handed in late.",
      "Late submission will incure additional charges."],
     "0,2",
     "incur 表示「招致、产生（费用）」，拼写为 incur（过去式 incurred）；"
     "charge fees 是被动转换后的同义表达。"),

    ("native", "这种植物是中国西南地区特有的。",
     ["This plant is native to southwest China.",
      "This plant is native of southwest China.",
      "The species is found only in the south-western part of China.",
      "This plant is native in southwest China."],
     "0,2",
     "native to 表示「原产于、为……所特有」，介词固定用 to；"
     "换一种说法可用 be found only in（只在某地能见到）。"),

    ("induce", "过度疲劳会导致注意力下降。",
     ["Excessive fatigue can induce a decline in concentration.",
      "Excessive fatigue can induce to a decline in concentration.",
      "A fall in the ability to concentrate may be brought on by extreme tiredness.",
      "Excessive fatigue can induce a decline of concentration."],
     "0,2",
     "induce 是及物动词，直接接宾语；「注意力下降」用 a decline in concentration，介词用 in。"),

    ("indulge", "孩子们整个暑假都沉迷于电子游戏。",
     ["The children indulged in video games throughout the summer holiday.",
      "The children indulged with video games throughout the summer holiday.",
      "All summer long the children were addicted to computer games.",
      "The children indulged to video games throughout the summer holiday."],
     "0,2",
     "indulge in 表示「沉迷于」，介词用 in；be addicted to 是同义表达。"),

    ("inertia", "长期形成的习惯有一种难以克服的惰性。",
     ["Long-established habits have a kind of inertia that is hard to overcome.",
      "Long-established habits have a kind of inertia that is hard to overcome with.",
      "There is a resistance to change in habits formed over many years.",
      "Long-established habits have a kind of inert that is hard to overcome."],
     "0,2",
     "inertia 是名词「惰性、惯性」；inert 是形容词「惰性的」，不能作 have 的宾语。"
     "that 从句中 overcome 已有宾语 that，句尾不必再加 with。"),

    ("inherent", "这种方法本身存在着一定的局限性。",
     ["This method has certain inherent limitations.",
      "This method has certain inherent limitations of.",
      "Limitations are inherent in the method itself.",
      "This method has certain inherited limitations."],
     "0,2",
     "inherent 表示「固有的、内在的」；inherited 表示「继承来的」，语义不同。"
     "be inherent in 是固定搭配。"),

    ("inhibit", "过度的批评会抑制学生的表达欲。",
     ["Excessive criticism inhibits students' desire to express themselves.",
      "Excessive criticism inhibits students' desire expressing themselves.",
      "Students become less willing to speak out when criticised too much.",
      "Excessive criticism inhibits with students' desire to express themselves."],
     "0,2",
     "inhibit 是及物动词，直接接宾语；desire to do sth. 中的 to 是不定式符号，后接动词原形。"),

    ("initiate", "这个项目由几位青年教师发起。",
     ["The project was initiated by several young teachers.",
      "The project was initiated with several young teachers.",
      "It was a few young teachers who started the project.",
      "The project was initiated by several young teachers to."],
     "0,2",
     "initiate / start 表示「发起」；被动句中动作执行者用 by 引出，不能用 with。"),

    ("reform", "学校鼓励教师在教学方法上不断改进。",
     ["The school encourages teachers to keep reforming their teaching methods.",
      "The school encourages teachers to keep reform their teaching methods.",
      "Teachers are urged by the school to bring constant improvement to their teaching.",
      "The school encourages teachers to keep reforming their teaching methods to."],
     "0,2",
     "keep 后接动名词 reforming；名词形式是 bring improvement to ...。"),

    ("insight", "这本书让我对这个时代有了更深的了解。",
     ["This book has given me a deeper insight into the era.",
      "This book has given me a deeper insight of the era.",
      "Thanks to this book, I now understand the age much better.",
      "This book has given me a deeper sight into the era."],
     "0,2",
     "insight into sth. 表示「对……的洞见」；insight 不可替换成 sight（视力、景象）。"),
]
