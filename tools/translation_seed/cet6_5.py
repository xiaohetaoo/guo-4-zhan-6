# -*- coding: utf-8 -*-
"""六级真题句子翻译补充题（第 5 批）"""

STAGE = 2

ITEMS = [
    ("integral", "课外实践是整个教学体系中不可或缺的组成部分。",
     ["Out-of-class practice is an integral part of the whole teaching system.",
      "Out-of-class practice is an integral part of the whole teaching system for.",
      "The teaching system would be incomplete without practice outside the classroom.",
      "Out-of-class practice is an integrate part of the whole teaching system."],
     "0,2",
     "integral 表示「不可或缺的、构成整体所必需的」；integrate 是动词，integrated 才是形容词化的分词。"
     "an integral part of 是固定搭配。"),

    ("intensify", "随着考试临近，学生们的焦虑感明显加剧。",
     ["As the examination draws near, students' anxiety intensifies noticeably.",
      "As the examination draws near, students' anxiety is intensified noticeably.",
      "Students become visibly more anxious with the approach of the exam.",
      "As the examination draws near, students' anxiety intensifies noticeable."],
     "0,2",
     "intensify 表示「加剧」时可用主动语态（主语是 anxiety）；"
     "修饰动词要用副词 noticeably，不是形容词 noticeable。"),

    ("interact", "学生之间的互动对语言学习十分重要。",
     ["Interaction among students is essential to language learning.",
      "Interaction among students is essential for language learning.",
      "How learners interact with one another matters greatly in language study.",
      "Interaction among students is essential to language learning with."],
     "0,1",
     "essential to / for 均可；interact with 表示「与……互动」。"
     "句尾不可再加孤立介词 with。"),

    ("interfere", "过多的噪声会干扰孩子的注意力。",
     ["Too much noise interferes with children's concentration.",
      "Too much noise interferes children's concentration.",
      "Children find it harder to concentrate when there is a great deal of noise.",
      "Too much noise interferes in children's concentration."],
     "0,2",
     "interfere with sth. 表示「干扰、妨碍」；interfere in 表示「干涉（他人事务）」，语义不同；"
     "interfere 是不及物动词，不能直接接宾语。"),

    ("intervene", "老师及时介入，化解了两个班级之间的矛盾。",
     ["The teacher intervened in time and resolved the conflict between the two classes.",
      "The teacher intervened in time and resolved the conflict among the two classes.",
      "Timely intervention by the teacher settled the disagreement between the two classes.",
      "The teacher intervened with in time and resolved the conflict between the two classes."],
     "0,2",
     "intervene 表示「介入、干预」，作不及物动词时后面不直接加宾语；"
     "「两者之间」用 between，三者以上才用 among。名词形式是 intervention。"),

    ("intrinsic", "阅读本身就有其内在的价值。",
     ["Reading has intrinsic value in itself.",
      "Reading has intrinsic value of itself.",
      "There is value in reading that belongs to the activity itself.",
      "Reading has intrinsical value in itself."],
     "0,2",
     "intrinsic 表示「内在的、固有的」，没有 intrinsical 这种写法；"
     "in itself 表示「本身」，是固定短语。"),

    ("cite", "报告援引了近五年的调查数据。",
     ["The report cites data from surveys carried out in the last five years.",
      "The report cites data from surveys carrying out in the last five years.",
      "Survey data collected over the past five years are quoted in the report.",
      "The report cites to data from surveys carried out in the last five years."],
     "0,2",
     "cite / quote 表示「援引、引用」；「开展调查」中 surveys 与 carry out 是被动关系，"
     "要用过去分词 carried out。cite 是及物动词，不加 to。"),

    ("jeopardize", "频繁缺课会危及你的期末成绩。",
     ["Frequent absence from class will jeopardize your final grade.",
      "Frequent absence from class will jeopardize your final grade to.",
      "Your grade at the end of the term may be put at risk by missing classes often.",
      "Frequent absence from class will jeopard with your final grade."],
     "0,2",
     "jeopardize 是及物动词「危及」，直接接宾语；put sth. at risk 是同义表达。"
     "jeopard 不是规范用词。"),

    ("leverage", "学校希望利用校友资源为学生提供实习机会。",
     ["The school hopes to leverage its alumni resources to provide internships for students.",
      "The school hopes to leverage of its alumni resources to provide internships for students.",
      "By making use of former students, the school aims to find internships for current ones.",
      "The school hopes to leverage its alumni resources for provide internships for students."],
     "0,2",
     "leverage 作动词「利用」是及物的，直接接宾语；make use of 是名词化同义短语。"
     "provide 前用不定式 to，不用 for（for 后要接动名词 providing）。"),

    ("magnitude", "这次改革的影响之大超出了所有人的预料。",
     ["The magnitude of the impact of this reform exceeded everyone's expectations.",
      "The magnitude of the impact of this reform was exceeded everyone's expectations.",
      "No one had expected the reform to have an impact of such magnitude.",
      "The magnitude of the impact of this reform exceeded everyone's expectation."],
     "0,2",
     "exceed 作「超出」讲是及物的，不用被动；"
     "「所有人的预料」用复数 expectations（每个人的预期各不相同）。"),

    ("compulsory", "新生必须在开学前完成安全培训。",
     ["Safety training is compulsory for all freshmen before the term begins.",
      "Safety training is compulsory to all freshmen before the term begins.",
      "All new students are required to complete safety training before the semester starts.",
      "Safety training is compulsary for all freshmen before the term begins."],
     "0,2",
     "compulsory 表示「强制的、必修的」，拼写为 compulsory（不是 compulsary）；"
     "be compulsory for sb. 介词用 for。"),

    ("manifest", "他的不安在言谈中表露无遗。",
     ["His unease was clearly manifested in the way he spoke.",
      "His unease was clearly manifested on the way he spoke.",
      "The way he talked showed only too clearly how uneasy he felt.",
      "His unease was clearly manifesting in the way he spoke."],
     "0,2",
     "manifest 作动词「表露」，此处「不安被表露」用被动 was manifested；"
     "「在言谈中」用 in the way he spoke，不用 on。"),

    ("manipulate", "研究者需要谨慎处理这些原始数据。",
     ["The researchers need to handle these raw data with care.",
      "The researchers need to manipulate these raw data with care.",
      "Care must be taken when the raw data are processed.",
      "The researchers need to handle these raw data with careful."],
     "0,2",
     "handle / process data 表示「处理数据」；manipulate 在数据语境中常含「人为篡改」的贬义，不宜使用。"
     "with care 是固定短语，careful 是形容词。"),

    ("merge", "两所学院合并成了一个新的研究院。",
     ["The two colleges merged into a new research institute.",
      "The two colleges merged with a new research institute.",
      "A new research institute was formed by the merging of the two colleges.",
      "The two colleges were merged into a new research institute."],
     "0,2",
     "merge into 表示「合并成为（新实体）」；merge with 表示「与……合并」，后面应接原有的另一方。"
     "「两所学院合并」用主动语态 merged 即可。"),

    ("migrate", "许多候鸟每年在两地之间迁徙。",
     ["Many migratory birds migrate between the two places every year.",
      "Many migrated birds migrate between the two places every year.",
      "Every year a large number of birds travel from one region to the other and back.",
      "Many migratory birds migrate among the two places every year."],
     "0,2",
     "migratory birds 表示「候鸟」；migrated 是过去分词，表示「已经迁徙过的」，不合语境。"
     "「两地之间」用 between。"),

    ("minimize", "这种做法可以把风险降到最低。",
     ["This practice can minimize the risks involved.",
      "This practice can minimize the risks involving.",
      "Risks can be reduced to a minimum by doing things this way.",
      "This practice can minimum the risks involved."],
     "0,2",
     "minimize 是动词「使最小化」；minimum 是名词 / 形容词，不能作谓语。"
     "「所涉及的风险」用过去分词 involved 作后置定语。"),

    ("mobilize", "社区动员了大量志愿者参与环境整治。",
     ["The community mobilized a large number of volunteers to take part in the clean-up.",
      "The community mobilized a large number of volunteers taking part in the clean-up.",
      "Many volunteers were called on by the community to help improve the environment.",
      "The community mobilized a large number of volunteers for take part in the clean-up."],
     "0,2",
     "mobilize / call on sb. to do sth. 表示「动员某人做某事」，后接不定式；"
     "for 是介词，后应接动名词 taking。"),

    ("motivate", "内在的兴趣比外在的奖励更能激励学生。",
     ["Intrinsic interest motivates students more effectively than external rewards.",
      "Intrinsic interest motivates students more effective than external rewards.",
      "Students are more strongly motivated by inner interest than by outside rewards.",
      "Intrinsic interest motives students more effectively than external rewards."],
     "0,2",
     "motivate 是动词「激励」；motive 是名词「动机」（形容词为 motive power），不能作谓语。"
     "修饰动词用副词 effectively。"),

    ("multiply", "过去二十年，网民数量增加了数倍。",
     ["Over the past two decades the number of Internet users has multiplied.",
      "Over the past two decades the number of Internet users has been multiplied.",
      "The number of people online has increased several times in the last twenty years.",
      "Over the past two decades the number of Internet users has multiplying."],
     "0,2",
     "multiply 表示「成倍增加」时可用主动语态；has been multiplied 被动式在此不当，"
     "has multiplying 语法错误（应为 has been multiplying）。"),

    ("negligible", "两组之间的差异可以忽略不计。",
     ["The difference between the two groups is negligible.",
      "The difference between the two groups is neglectful.",
      "One can safely ignore the gap that separates the two groups.",
      "The difference between the two groups is neglected."],
     "0,2",
     "negligible 表示「微不足道、可忽略的」；neglectful 表示「疏忽的（形容人）」，"
     "neglected 表示「被忽视的」，两者都不合此处语境。"),

    ("notable", "这个地区的教育水平有了显著提高。",
     ["The educational level of this region has improved notably.",
      "The educational level of this region has improved notable.",
      "There has been a remarkable improvement in education in this area.",
      "The educational level of this region has improved noticeable."],
     "0,2",
     "修饰动词 improved 要用副词 notably；remarkable 是形容词，修饰名词 improvement。"
     "notable 是形容词，不能修饰动词。"),

    ("nourish", "阅读优秀的作品能滋养一个人的心灵。",
     ["Reading fine works nourishes a person's mind.",
      "Reading fine works nourishes a person's mind to.",
      "A person's inner life is enriched by good reading.",
      "Reading fine works nourish a person's mind."],
     "0,2",
     "动名词短语作主语时谓语用单数 nourishes；nourish / enrich 表示「滋养、丰富」。"),

    ("notorious", "这条街因交通拥堵而众所周知。",
     ["This street is notorious for its traffic jams.",
      "This street is notorious of its traffic jams.",
      "The street is well known for being constantly jammed with traffic.",
      "This street is notorious with its traffic jams."],
     "0,2",
     "notorious for 表示「因……而臭名昭著 / 出了名的」，介词固定用 for；"
     "well known for 是中性的同义表达。"),

    ("block", "停放的自行车堵住了教学楼的入口。",
     ["The parked bicycles blocked the entrance to the teaching building.",
      "The parked bicycles blocked the entrance of the teaching building.",
      "The way into the teaching building was blocked by bicycles left there.",
      "The parked bicycles blocked to the entrance to the teaching building."],
     "0,2",
     "block 是及物动词，直接接宾语；「……的入口」用 the entrance to，介词用 to。"),

    ("offset", "节能措施部分抵消了用电量的增长。",
     ["Energy-saving measures have partly offset the growth in electricity consumption.",
      "Energy-saving measures have partly offset with the growth in electricity consumption.",
      "The rise in power use has been partly compensated for by measures to save energy.",
      "Energy-saving measures have partly offset to the growth in electricity consumption."],
     "0,2",
     "offset 是及物动词，直接接宾语；compensate for 表示「弥补」，介词 for 不能省。"),

    ("refine", "工程师们正在改进这套调度算法。",
     ["The engineers are refining this scheduling algorithm.",
      "The engineers are refining this scheduling algorithm to.",
      "Work is under way to make the scheduling algorithm more efficient.",
      "The engineers are refinement this scheduling algorithm."],
     "0,2",
     "refine 是动词「改进、优化」；refinement 是名词，不能作谓语。"
     "现在进行时结构是 be + 现在分词 refining。"),
]
