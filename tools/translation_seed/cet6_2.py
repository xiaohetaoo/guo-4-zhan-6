# -*- coding: utf-8 -*-
"""六级真题句子翻译补充题（第 2 批）"""

STAGE = 2

ITEMS = [
    ("consolidate", "期末复习是巩固一学期所学内容的关键环节。",
     ["Revision at the end of the term is a key step in consolidating what has been learnt during the semester.",
      "Revision at the end of the term is a key step to consolidate what has been learnt during the semester.",
      "What has been taught over the term is reinforced mainly through end-of-term review.",
      "Revision at the end of the term is a key step in consolidating what has been learnt during the semester for."],
     "0,2",
     "consolidate / reinforce 表示「巩固、强化」；a key step in doing sth. 中的 in 是介词，后接动名词。"
     "句尾不可再接孤立介词 for。"),

    ("conspicuous", "这座白色教学楼在灰旧的街区中格外显眼。",
     ["The white teaching building is conspicuous against the grey old neighbourhood.",
      "The white teaching building is conspicuous in the grey old neighbourhood.",
      "Standing out sharply from the drab grey streets, the white school building catches every eye.",
      "The white teaching building is conspicious against the grey old neighbourhood."],
     "0,2",
     "conspicuous 表示「显眼的」，注意拼写是 -uous 不是 -icious；"
     "「在……衬托下显眼」用 against，强调对比；in 只是表示位置，不含对比意味。"),

    ("contemplate", "他坐在窗前，思考着毕业后的方向。",
     ["He sat by the window, contemplating what he would do after graduation.",
      "He sat by the window, contemplating to do what after graduation.",
      "Sitting at the window, he reflected on the direction his life would take after leaving school.",
      "He sat by the window, contemplated what he would do after graduation."],
     "0,2",
     "contemplate 后接名词或 wh- 从句，不接不定式；"
     "逗号后的现在分词 contemplating 作伴随状语，用过去分词 contemplated 语法错误。"),

    ("contend", "一些学者认为，阅读习惯的变化比词汇量的变化更值得关注。",
     ["Some scholars contend that changes in reading habits deserve more attention than changes in vocabulary size.",
      "Some scholars contend changes in reading habits deserve more attention than changes in vocabulary size.",
      "It is argued by some scholars that how people read matters more than how many words they know.",
      "Some scholars contend with that changes in reading habits deserve more attention."],
     "0,2",
     "contend that 引导宾语从句时，that 在正式语体中不宜省略；"
     "contend with 表示「应付、与……抗争」，不能接 that 从句。"),

    ("contradict", "这两份调查的结果相互矛盾，需要进一步核实。",
     ["The results of the two surveys contradict each other and need to be further checked.",
      "The results of the two surveys are contradicted each other and need to be further checked.",
      "Since the two sets of findings are in contradiction with each other, further verification is required.",
      "The results of the two surveys contradict with each other and need to be further checked."],
     "0,2",
     "contradict 作及物动词时直接接宾语，不加 with，也不用被动（相互矛盾用 contradict each other）；"
     "名词形式是 be in contradiction with。"),

    ("controversial", "这一教学改革的措施在教育界引起了争议。",
     ["This measure of teaching reform has proved controversial in educational circles.",
      "This measure of teaching reform has proved controversy in educational circles.",
      "The reform in teaching has given rise to much debate among educators.",
      "This measure of teaching reform has proved controversial in educational circles of."],
     "0,2",
     "prove 作系动词后接形容词 controversial；controversy 是名词。"
     "give rise to debate 表示「引发争议」。"),

    ("converge", "来自不同学科的研究者在这里汇聚一堂。",
     ["Researchers from different disciplines converge here.",
      "Researchers from different disciplines converge to here.",
      "This is where scholars from a variety of fields come together.",
      "Researchers from different disciplines are converged here."],
     "0,2",
     "converge 表示「汇聚」，here 是副词，前面不加 to；"
     "converge 作「聚集」讲不用被动语态。come together 是口语化同义表达。"),

    ("correlate", "研究显示，睡眠时间与学习成绩呈正相关。",
     ["The study shows that hours of sleep correlate positively with academic performance.",
      "The study shows that hours of sleep correlate positively to academic performance.",
      "There is a positive correlation between the amount of sleep and school achievement.",
      "The study shows that hours of sleep are correlated positively with academic performance to."],
     "0,2",
     "correlate with 与名词形式 correlation between ... and ... 是标准搭配；"
     "句尾孤立介词 to 使句子残缺。"),

    ("counterpart", "中国学生对这一问题的看法与他们的美国同龄人不同。",
     ["Chinese students' views on this issue differ from those of their American counterparts.",
      "Chinese students' views on this issue differ from their American counterparts.",
      "On this question, students in China do not see eye to eye with students of the same age in the United States.",
      "Chinese students' views on this issue differ with those of their American counterparts."],
     "0,2",
     "比较的对象是「看法」而不是「人」，要用 those of their counterparts；"
     "differ from 是固定搭配（differ with 表示「与某人意见不同」，后接人）。"),

    ("criterion", "录取与否不应只以分数作为唯一标准。",
     ["Scores alone should not be the sole criterion for admission.",
      "Scores alone should not be the sole criteria for admission.",
      "Whether an applicant is admitted ought not to be judged only by marks.",
      "Scores alone should not be the sole criterion of admission to."],
     "0,2",
     "criterion 是单数，criteria 是复数；sole 后接单数。criterion for 表示「……的标准」。"),

    ("cumulative", "每天记十个单词，一年下来累积的效果相当可观。",
     ["Learning ten words a day produces a considerable cumulative effect over a year.",
      "Learning ten words a day produces a considerable cumulative effect over a year for.",
      "Over a year, the accumulated effect of memorising ten words a day is quite remarkable.",
      "Learning ten words a day produces a considerable accumulate effect over a year."],
     "0,2",
     "cumulative / accumulated 表示「累积的」；accumulate 是动词，不能作定语修饰 effect。"),

    ("curb", "学校采取了一系列措施来遏制浪费行为。",
     ["The school has adopted a series of measures to curb wasteful behaviour.",
      "The school has adopted a series of measures to curb wasteful behaviour to.",
      "A number of steps have been taken by the school in order to check wasteful practices.",
      "The school has adopted a series of measures to curb with wasteful behaviour."],
     "0,2",
     "curb / check 作「遏制」讲是及物动词，直接接宾语；句尾不可再加 to。"),

    ("decisive", "家长的态度对孩子的阅读习惯起着决定性的作用。",
     ["Parents' attitudes play a decisive role in shaping children's reading habits.",
      "Parents' attitudes play a decisive role to shape children's reading habits.",
      "Whether a child becomes a reader depends decisively on the attitude of the parents.",
      "Parents' attitudes play a decided role in shaping children's reading habits."],
     "0,2",
     "play a role in doing sth. 中的 in 是介词，后接动名词；"
     "decisive 表示「决定性的」，decided 表示「明确的、显然的」，语义不同。"),

    ("deduce", "从这些数据中，我们可以推断出用户的偏好。",
     ["From these data we can deduce the preferences of users.",
      "From these data we can deduce of the preferences of users.",
      "Users' preferences can be inferred from the data collected.",
      "From these data we can deduce to the preferences of users."],
     "0,2",
     "deduce / infer 是及物动词，直接接宾语；deduce from 表示「从……推断」。"
     "两者后都不接 to 或 of。"),

    ("deficit", "这家小型出版社长期处于亏损状态。",
     ["The small publishing house has been running at a deficit for a long time.",
      "The small publishing house has been running in a deficit for a long time.",
      "For years the small publisher has been operating in the red.",
      "The small publishing house has been running at a deficit for a long time for."],
     "0,2",
     "run at a deficit / operate in the red 都表示「亏损经营」；介词用 at，不用 in。"
     "for a long time 已表时长，句尾不必再加 for。"),

    ("deliberate", "这次课程调整是经过深思熟虑之后作出的决定。",
     ["The adjustment of the curriculum was made after deliberate consideration.",
      "The adjustment of the curriculum was made after deliberate considering.",
      "It was only after careful thought that the decision to change the courses was taken.",
      "The adjustment of the curriculum was made after deliberately consideration."],
     "0,2",
     "deliberate 是形容词「深思熟虑的」，修饰名词 consideration；"
     "consideration 是名词，不可用 considering（介词 / 连词）；deliberately 是副词，不能作定语。"),

    ("denote", "图中的虚线表示预测值，实线表示实测值。",
     ["In the figure, the dashed line denotes predicted values and the solid line denotes measured ones.",
      "In the figure, the dashed line denotes to predicted values and the solid line denotes measured ones.",
      "Predicted values are shown by the dotted line in the chart, while measured ones by the solid line.",
      "In the figure, the dashed line denotes predicted values and the solid line denotes measured one."],
     "0,2",
     "denote 是及物动词，直接接宾语，不加 to；"
     "ones 指代复数 values，不能用 one。"),

    ("depict", "这幅壁画描绘了古代集市上买卖交易的场景。",
     ["The mural depicts scenes of buying and selling in an ancient market.",
      "The mural depicts of scenes of buying and selling in an ancient market.",
      "What the wall painting shows is how people traded in a market in ancient times.",
      "The mural depicts scenes of buying and selling in an ancient market for."],
     "0,2",
     "depict / show 是及物动词，直接接宾语；句尾不可再接孤立介词。"),

    ("designate", "这块区域被指定为校园的安静学习区。",
     ["This area has been designated as a quiet study zone on campus.",
      "This area has been designated a quiet study zone on campus.",
      "The zone is set aside for silent study by students on campus.",
      "This area has been designated for a quiet study zone on campus."],
     "0,1",
     "designate sth. as sth. 与 designate sth. sth.（双宾语）都成立；"
     "designate ... for ... 表示「为某目的指定」，此处后面是身份名词，应用 as。"),

    ("discourage", "高昂的费用并没有阻挡他继续求学的决心。",
     ["The high cost did not discourage him from continuing his studies.",
      "The high cost did not discourage him to continue his studies.",
      "His determination to keep studying was not weakened by the heavy expense.",
      "The high cost did not discourage him continuing his studies from."],
     "0,2",
     "discourage sb. from doing sth. 表示「劝阻 / 使某人不敢做某事」，介词 from 不能省，也不接不定式。"),

    ("deviate", "实验结果与预期值出现了明显偏离。",
     ["The experimental results deviate markedly from the expected values.",
      "The experimental results deviate markedly with the expected values.",
      "There is a marked deviation between what was observed and what had been expected.",
      "The experimental results are deviated markedly from the expected values."],
     "0,2",
     "deviate from 是固定搭配，deviate 作「偏离」讲不用被动语态；"
     "名词形式是 deviation between ... and ...。"),

    ("distinguish", "这款软件可以区分含义相近的词。",
     ["This software can distinguish between words that have very similar meanings.",
      "This software can distinguish among words that have very similar meanings.",
      "The program is able to tell fine distinctions among words of similar meaning.",
      "This software can distinguish with words that have very similar meanings."],
     "0,2",
     "distinguish between A and B 表示「区分两者」；tell distinctions among ... 是同义表达。"
     "distinguish 是不及物动词，不加 with；两者之间用 between，among 用于三者以上。"),

    ("diffuse", "新知识在群体中的传播往往比想象中慢。",
     ["The diffusion of new knowledge through a group is often slower than imagined.",
      "The diffusion of new knowledge through a group is often slower than imagining.",
      "New ideas spread among a community more slowly than one would expect.",
      "The diffuse of new knowledge through a group is often slower than imagined."],
     "0,2",
     "diffuse 是动词 / 形容词，名词「传播」是 diffusion；"
     "than imagined 相当于 than it is imagined，省略结构中用过去分词表被动。"),

    ("dimension", "这项研究为该问题提供了一个全新的维度。",
     ["This study offers a completely new dimension to the issue.",
      "This study offers a completely new dimension of the issue.",
      "Thanks to this research, the problem can now be approached from a fresh angle.",
      "This study offers a completely new dimensional to the issue."],
     "0,2",
     "dimension to sth. 表示「看待某事物的维度」；dimensional 是形容词，不能作宾语。"),

    ("disclose", "未经允许，不得透露参与者的个人信息。",
     ["Personal information about participants must not be disclosed without permission.",
      "Personal information about participants must not disclose without permission.",
      "No one may reveal the personal details of the participants unless permitted.",
      "Personal information about participants must not be disclosed without permission to."],
     "0,2",
     "「信息被透露」用被动 be disclosed / be revealed；句尾不可再接孤立介词 to。"),

    ("discrepancy", "两组实验数据之间存在明显出入。",
     ["There is an obvious discrepancy between the two sets of experimental data.",
      "There is an obvious discrepancy among the two sets of experimental data.",
      "The two groups of data differ from each other in a noticeable way.",
      "There is an obvious discreet between the two sets of experimental data."],
     "0,2",
     "discrepancy between A and B 用于两者之间；discreet 意为「谨慎的」，与 discrepancy 形近义不同。"),
]
