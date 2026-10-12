# -*- coding: utf-8 -*-
"""六级真题句子翻译补充题（第 1 批）

六级翻译真题的句子更长、句式更复杂，本批题干按真题风格写成两三小句，
选项中的错误也更隐蔽（固定搭配、逻辑关系、语体是否恰当）。
"""

STAGE = 2

ITEMS = [
    ("adhere", "所有参与实验的人员都必须严格遵守操作规程。",
     ["All personnel involved in the experiment must strictly adhere to the operating procedures.",
      "All personnel involved in the experiment must strictly adhere with the operating procedures.",
      "Everyone taking part in the experiment is required to comply strictly with the operating rules.",
      "All personnel involved in the experiment must strictly adhere the operating procedures."],
     "0,2",
     "adhere to 与 comply with 都表示「遵守」，介词分别是 to 与 with，不可混用；"
     "adhere 是不及物动词，不能直接接宾语。"),

    ("adjacent", "新建的实验楼紧挨着图书馆，师生往来十分方便。",
     ["The newly built laboratory building is adjacent to the library, which is very convenient for teachers and students.",
      "The newly built laboratory building is adjacent with the library, which is very convenient for teachers and students.",
      "Situated next to the library, the new lab building gives teachers and students easy access to both.",
      "The newly built laboratory building is adjacent the library, which is very convenient for teachers and students."],
     "0,2",
     "adjacent to 是固定搭配，形容词后必须带 to；也可用 next to / situated next to。"
     "adjacent 不能直接接宾语。"),

    ("adverse", "持续的阴雨天气对农作物的生长产生了不利影响。",
     ["Continuous rainy weather has had an adverse effect on the growth of crops.",
      "Continuous rainy weather has had an adverse effect to the growth of crops.",
      "The unbroken spell of rain has adversely affected how the crops are growing.",
      "Continuous rainy weather has had a reverse effect on the growth of crops."],
     "0,2",
     "have an adverse effect on sth. 介词用 on；也可用副词形式 adversely affect。"
     "reverse 意为「相反的、逆转的」，不是「不利的」。"),

    ("overall", "综合来看，这些数据反映出居民消费结构的整体变化。",
     ["Taken together, these figures reflect the overall change in the consumption structure of residents.",
      "Taken together, these figures reflect the overalled change in the consumption structure of residents.",
      "Viewed as a whole, the data show how the pattern of household consumption has shifted.",
      "Taken together, these figures reflect the overall change of the consumption structure of residents."],
     "0,2",
     "overall 是形容词「总体的」，本身没有 -ed 形式；viewed as a whole 是过去分词短语作状语，"
     "与 data 构成被动关系。「在……方面的变化」用 change in，不用 change of。"),

    ("analogy", "作者用河流作类比，来解释信息的流动方式。",
     ["The author draws an analogy with a river to explain how information flows.",
      "The author draws an analogy to a river to explain how information flows.",
      "By comparing information to a river, the author illustrates the way it moves.",
      "The author draws an analogy of a river to explain how information flows."],
     "0,2",
     "draw an analogy with / between 是固定搭配；也可用 compare A to B。"
     "analogy 后不接 of。"),

    ("arbitrary", "评分标准不应当是随意的，否则难以服众。",
     ["The grading criteria should not be arbitrary; otherwise it will be hard to convince anyone.",
      "The grading criteria should not be arbitrary; otherwise it will be hard to convince anyone of.",
      "If the marking standards were set at random, people would hardly accept the result.",
      "The grading criteria should not be arbitrary; otherwise it will be hardly to convince anyone."],
     "0,2",
     "arbitrary 表示「随意的、武断的」；at random 是副词短语，可作状语。"
     "convince 是及物动词，句尾不加 of；hardly 应紧靠被修饰的部分，不作表语补足。"),

    ("articulate", "她能清晰地说出自己的想法，这在讨论中很有优势。",
     ["She is able to articulate her ideas clearly, which is a great advantage in discussion.",
      "She is able to articulate her ideas clearly, that is a great advantage in discussion.",
      "Putting her thoughts into words clearly gives her a real edge in group discussions.",
      "She is able to articulate with her ideas clearly, which is a great advantage in discussion."],
     "0,2",
     "articulate 作动词「清楚表达」是及物的，直接接宾语；"
     "非限制性定语从句用 which 引导，不能用 that。"),

    ("ascertain", "警方仍在设法弄清事故发生时的确切情况。",
     ["The police are still trying to ascertain exactly what happened at the time of the accident.",
      "The police are still trying to ascertain exactly what had happened at the time of the accident.",
      "Efforts are still being made to establish the precise circumstances of the accident.",
      "The police are still trying to ascertain about what happened at the time of the accident."],
     "0,2",
     "ascertain 是及物动词，后接 wh- 从句或名词，不加 about；"
     "「事故发生时」是过去某个时间点正在发生的事，用一般过去时 what happened，过去完成时在此不当。"),

    ("ambition", "他一直渴望成为一名能改变乡村教育的人。",
     ["He has always had the ambition to become someone who can change rural education.",
      "He has always had the ambition becoming someone who can change rural education.",
      "It has long been his dream to be a person capable of transforming village schooling.",
      "He has always had the ambition to becoming someone who can change rural education."],
     "0,2",
     "ambition to do sth. 中的 to 是不定式符号，后接动词原形；接名词时用 ambition for / of。"
     "dream 可作同义替换（one's dream to do）。"),

    ("augment", "学校计划通过引入在线课程来充实现有教学资源。",
     ["The school plans to augment its existing teaching resources by introducing online courses.",
      "The school plans to augment its existing teaching resources with introducing online courses.",
      "Online courses are to be introduced so as to enrich the teaching resources already available.",
      "The school plans to augment its existing teaching resources by introduce online courses."],
     "0,2",
     "augment / enrich 表示「充实、扩充」；by 引出方式时后接动名词 introducing，"
     "by introduce 语法错误。"),

    ("authentic", "这本书里记录的是真实发生过的乡村故事。",
     ["The stories recorded in this book are authentic accounts of village life.",
      "The stories recorded in this book are authentic accounts of village life for.",
      "What is written in the book is a true record of things that really happened in the countryside.",
      "The stories recorded in this book are authority accounts of village life."],
     "0,2",
     "authentic 表示「真实可信的」；authority 是名词「权威」，不能作定语形容 accounts。"
     "「记录下来的故事」用过去分词 recorded 作定语。"),

    ("bias", "研究者在取样时应尽量避免任何偏见。",
     ["Researchers should try to avoid any bias when sampling.",
      "Researchers should try to avoid any bias when sampling to.",
      "Every effort should be made to keep the sample free of prejudice.",
      "Researchers should try to avoid any biased when sampling."],
     "0,2",
     "avoid 后接名词 bias；biased 是形容词（a biased sample），不能作 avoid 的宾语。"
     "free of 表示「免于……」。"),

    ("coherent", "这篇论文逻辑清晰，各部分衔接自然。",
     ["The paper is logically coherent, with its parts connected naturally.",
      "The paper is logically coherence, with its parts connected naturally.",
      "Clear in logic, the thesis holds together well from section to section.",
      "The paper is logically coherent, with its parts connecting naturally."],
     "0,2",
     "coherent 是形容词，coherence 是名词；be 后接形容词。"
     "「各部分被衔接」表状态，用过去分词 connected，现在分词 connecting 表主动进行。"),

    ("coincide", "考试周恰好与校园艺术节的时间重合。",
     ["The exam week coincides with the time of the campus art festival.",
      "The exam week coincides to the time of the campus art festival.",
      "The period of examinations happens to fall at the same time as the campus arts festival.",
      "The exam week is coincided with the time of the campus art festival."],
     "0,2",
     "coincide with 是固定搭配，且 coincide 没有被动语态；"
     "也可用 fall at the same time as。"),

    ("collaborate", "两所大学的团队将合作开展这项为期三年的研究。",
     ["The teams from the two universities will collaborate on this three-year research project.",
      "The teams from the two universities will collaborate with this three-year research project.",
      "This three-year study will be carried out jointly by teams from both universities.",
      "The teams from the two universities will collaborate to this three-year research project."],
     "0,2",
     "collaborate on a project 表示「就某项目合作」；collaborate with 后接合作的人，不接项目。"
     "也可用 do sth. jointly。"),

    ("commence", "新学期的第一堂课将于下周一正式开始。",
     ["The first class of the new semester will commence next Monday.",
      "The first class of the new semester will commence on next Monday.",
      "Classes for the new term are due to begin formally next Monday.",
      "The first class of the new semester will be commenced next Monday."],
     "0,2",
     "commence / begin 表示「开始」，作「开始」讲时不用被动语态 be commenced；"
     "next Monday 前不加介词 on。"),

    ("commend", "校长在大会上赞扬了志愿者们的奉献精神。",
     ["The principal commended the volunteers for their devotion at the meeting.",
      "The principal commended the volunteers of their devotion at the meeting.",
      "At the conference the headmaster praised the volunteers for their dedication.",
      "The principal commended the volunteers with their devotion at the meeting."],
     "0,2",
     "commend / praise sb. for sth. 表示「因某事赞扬某人」，介词固定用 for。"),

    ("compile", "他花了两年时间编成这部方言词典。",
     ["He spent two years compiling this dictionary of dialects.",
      "He spent two years to compile this dictionary of dialects.",
      "It took him two years to put together this dialect dictionary.",
      "He spent two years compiling this dictionary of dialects for."],
     "0,2",
     "spend time (in) doing sth. 是固定结构，不接不定式；compile / put together 表示「编纂」。"
     "句尾孤立介词 for 使句子残缺。"),

    ("complement", "课后练习与课堂讲解互为补充，缺一不可。",
     ["After-class exercises and classroom explanations complement each other; neither can be dispensed with.",
      "After-class exercises and classroom explanations compliment each other; neither can be dispensed with.",
      "The practice done after class and the teaching in class are complementary and equally necessary.",
      "After-class exercises and classroom explanations complement with each other; neither can be dispensed with."],
     "0,2",
     "complement 是「补充、互为补充」，compliment 是「赞美」，两者极易用混；"
     "complement 作及物动词时直接接宾语，不加 with。形容词形式为 complementary。"),

    ("comprehensive", "这份报告对城市交通问题作了全面的分析。",
     ["The report gives a comprehensive analysis of urban transport problems.",
      "The report gives a comprehensive analysis to urban transport problems.",
      "Urban traffic issues are analysed in a thorough manner in this report.",
      "The report gives a comprehended analysis of urban transport problems."],
     "0,2",
     "analysis of sth. 介词用 of；comprehensive 表示「全面的」，"
     "comprehended 是 comprehend（理解）的过去分词，语义不符。"),

    ("compromise", "双方都不愿在质量标准上作出妥协。",
     ["Neither side is willing to compromise on quality standards.",
      "Neither side is willing to compromise to quality standards.",
      "Both parties refuse to make any compromise over the standards of quality.",
      "Neither side is willing to compromise with quality standards."],
     "0,2",
     "compromise on / over sth. 表示「在某问题上妥协」；compromise with 后接妥协的对象（人）。"),

    ("conceive", "很难想象没有电的日常生活会是什么样子。",
     ["It is hard to conceive of daily life without electricity.",
      "It is hard to conceive daily life without electricity.",
      "One can hardly imagine what everyday life would be like without power.",
      "It is hard to conceive of daily life without electric."],
     "0,2",
     "conceive of 表示「想象、设想」（不及物，需 of）；imagine 是及物的，直接接宾语。"
     "「电」是名词 electricity，electric 是形容词。"),

    ("condense", "请把这篇三千字的报告浓缩成一页摘要。",
     ["Please condense this three-thousand-word report into a one-page summary.",
      "Please condense this three-thousand-word report to a one-page summary.",
      "This 3,000-word report needs to be compressed into a single page of abstract.",
      "Please condense this three-thousand-word report into a one-page summary with."],
     "0,2",
     "condense / compress ... into ... 表示「把……压缩成……」；"
     "三千字作定语时用连字符且 word 用单数（three-thousand-word）。"),

    ("confer", "学校将向优秀毕业生授予荣誉证书。",
     ["The school will confer certificates of honour on outstanding graduates.",
      "The school will confer certificates of honour to outstanding graduates.",
      "Certificates of honour are to be awarded to graduates who have performed excellently.",
      "The school will confer outstanding graduates with certificates of honour."],
     "0,2",
     "confer sth. on sb. 表示「授予某人某物」；award sth. to sb. 是同义结构。"
     "confer sb. with sth. 不是规范搭配。"),

    ("conform", "所有实验数据都必须符合既定的标准。",
     ["All experimental data must conform to the established standards.",
      "All experimental data must conform with the established standards.",
      "Every piece of data from the experiment has to be in line with the set standards.",
      "All experimental data must conform the established standards."],
     "0,2",
     "conform to 是固定搭配；be in line with 表示「与……一致」。"
     "conform 是不及物动词，不能直接接宾语。"),
]
