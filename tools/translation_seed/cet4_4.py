# -*- coding: utf-8 -*-
"""四级真题句子翻译补充题（第 4 批）"""

STAGE = 1

ITEMS = [
    ("distribute", "志愿者们把募集来的图书分发给了山村小学。",
     ["The volunteers distributed the donated books among village primary schools.",
      "The volunteers distributed the donated books between village primary schools.",
      "Books raised by the volunteers were handed out to primary schools in the mountain villages.",
      "The volunteers distributed the donated books to village primary schools among."],
     "0,2",
     "「在多所学校之间分发」数量大于二，用 among；between 用于两者之间。"
     "hand out / distribute sth. to sb. 是常见表达，句尾不可再加孤立介词。"),

    ("diverse", "这个城市的饮食文化非常多元。",
     ["The food culture of this city is very diverse.",
      "The food culture of this city is very diversity.",
      "This city boasts a highly diversified food culture.",
      "The food culture of this city is very various."],
     "0,2",
     "diverse 是形容词「多样的」，diversity 是名词；diversified 可作定语。"
     "various 强调「各种各样」，描述整体文化的多元性以 diverse / diversified 更准确。"),

    ("dominate", "英语在国际贸易中占据主导地位。",
     ["English dominates international trade.",
      "English is dominated international trade.",
      "English plays a dominant role in international trade.",
      "English dominates in international trade with."],
     "0,2",
     "dominate 是及物动词，直接接宾语（dominate sth.），不用被动也不用加 in；"
     "也可用 play a dominant role in。"),

    ("eager", "大一新生们都渴望参加各种社团活动。",
     ["Freshmen are eager to take part in various club activities.",
      "Freshmen are eager for take part in various club activities.",
      "All the freshmen are keen on joining different student societies.",
      "Freshmen are eager taking part in various club activities."],
     "0,2",
     "be eager to do 与 be keen on doing 都表示「渴望 / 热衷于」；"
     "be eager for 后接名词（be eager for success），不接动词原形，eager 也不接动名词。"),

    ("economic", "旅游业的发展给这个小镇带来了巨大的经济效益。",
     ["The development of tourism has brought huge economic benefits to the small town.",
      "The development of tourism has brought huge economical benefits to the small town.",
      "Tourism has generated considerable economic gains for this small town.",
      "The development of tourism has brought huge economy benefits to the small town."],
     "0,2",
     "economic 指「经济（方面）的」，economical 指「节约的」，两者完全不同；"
     "economy 是名词，不能直接修饰 benefits。"),

    ("educate", "教育的目的不只是传授知识。",
     ["The purpose of education is not merely to impart knowledge.",
      "The purpose of education is not merely imparting knowledge.",
      "Education aims at more than passing on knowledge.",
      "The purpose of educate is not merely to impart knowledge."],
     "0,2",
     "purpose / aim 作主语时，表语用不定式 to do；也可用 aim at doing。"
     "of 后接名词 education，不能用动词原形 educate。"),

    ("eliminate", "这项措施有望在三年内消除校园里的浪费现象。",
     ["This measure is expected to eliminate waste on campus within three years.",
      "This measure is expected to eliminate waste on campus within three years to.",
      "It is hoped that the measure will do away with campus waste in three years.",
      "This measure is hoped to eliminate waste on campus within three years."],
     "0,2",
     "be expected to do 表示「有望……」；measure 本身不会「希望」，"
     "说 is hoped to 逻辑主语错误，应改为 It is hoped that ...。"),

    ("emerge", "一批年轻的本土设计师正在崭露头角。",
     ["A group of young local designers is emerging.",
      "A group of young local designers is emerging from.",
      "Young home-grown designers are coming to the fore.",
      "A group of young local designers is immersing."],
     "0,2",
     "emerge 表示「崭露头角、兴起」，come to the fore 是同义短语；"
     "emerge from 后需接具体出处。immerse 意为「沉浸」，与 emerge 形近义不同。"),

    ("employ", "这家公司今年雇用了两百多名应届毕业生。",
     ["The company employed more than two hundred new graduates this year.",
      "The company employed more than two hundred new graduates to this year.",
      "Over two hundred fresh graduates were hired by the company this year.",
      "The company employed more than two hundred new graduates in this year."],
     "0,2",
     "employ / hire 都表示「雇用」；this year 作时间状语时前面不加介词 in 或 to。"),

    ("enable", "智能手机使人们随时随地都能获取信息。",
     ["Smartphones enable people to get information anytime and anywhere.",
      "Smartphones enable people getting information anytime and anywhere.",
      "With smartphones, people can obtain information whenever and wherever they like.",
      "Smartphones enable that people get information anytime and anywhere."],
     "0,2",
     "enable sb. to do sth. 是固定结构，不接动名词，也不接 that 从句。"),

    ("encounter", "在实习过程中，他遇到了许多意想不到的困难。",
     ["During his internship he encountered many unexpected difficulties.",
      "During his internship he encountered with many unexpected difficulties.",
      "He came across a lot of unforeseen problems while doing his internship.",
      "During his internship he encountered many unexpected difficult."],
     "0,2",
     "encounter 是及物动词，直接接宾语，不加 with；come across 是同义表达。"
     "「困难」是名词 difficulties，difficult 是形容词。"),

    ("endure", "登山队员们忍受着严寒继续往上爬。",
     ["The climbers endured the severe cold and kept climbing.",
      "The climbers endured the severe cold to keep climbing.",
      "Despite the bitter cold, the mountaineers pressed on upwards.",
      "The climbers endured with the severe cold and kept climbing."],
     "0,2",
     "endure 是及物动词，直接接宾语（endure the cold）；and 连接两个并列谓语 kept，"
     "不能改成不定式 to keep。"),

    ("engage", "越来越多的年轻人参与到志愿服务中来。",
     ["More and more young people engage in volunteer service.",
      "More and more young people engage with volunteer service.",
      "A growing number of young people are involved in voluntary work.",
      "More and more young people engage to volunteer service."],
     "0,2",
     "engage in 与 be involved in 都表示「参与」；engage with 意为「与……互动」，语义不同；"
     "engage to 不是正确搭配。"),

    ("ensure", "请务必确保离开教室时关掉所有电器。",
     ["Please make sure that all the electrical appliances are switched off when you leave the classroom.",
      "Please ensure all the electrical appliances switching off when you leave the classroom.",
      "Be sure to turn off every electrical device before leaving the classroom.",
      "Please ensure to all the electrical appliances are switched off when you leave."],
     "0,2",
     "make sure / ensure that 后接完整从句；be sure to do 表示「务必做」。"
     "ensure 不接「宾语 + 现在分词」，也不说 ensure to that。"),

    ("enthusiasm", "他对天文学的热情从未减退。",
     ["His enthusiasm for astronomy has never faded.",
      "His enthusiasm of astronomy has never faded.",
      "He has never lost his passion for astronomy.",
      "His enthusiasm to astronomy has never faded."],
     "0,2",
     "enthusiasm / passion for sth. 介词用 for，不用 of 或 to；fade 表示「减退」。"),

    ("equip", "所有的实验室都配备了新的安全设备。",
     ["All the laboratories are equipped with new safety equipment.",
      "All the laboratories are equipped by new safety equipment.",
      "Every lab has been fitted out with new safety facilities.",
      "All the laboratories equip with new safety equipment."],
     "0,2",
     "be equipped with 表示「配备有……」，介词 with 引出配备物；"
     "实验室是被配备的对象，要用被动语态，主动式 equip with 缺少宾语。"),

    ("essential", "充足的睡眠对青少年的成长至关重要。",
     ["Enough sleep is essential to the growth of teenagers.",
      "Enough sleep is essential for the growth of teenagers.",
      "Adequate sleep is of vital importance to adolescent development.",
      "Enough sleep is essential with the growth of teenagers."],
     "0,1",
     "be essential to / for 都表示「对……必不可少」；也可用 be of vital importance to。"
     "essential 后不接 with。"),

    ("estimate", "据估计，这座桥每天通行车辆超过五万辆。",
     ["It is estimated that more than fifty thousand vehicles cross the bridge every day.",
      "It is estimated more than fifty thousand vehicles cross the bridge every day.",
      "The bridge is estimated to carry over 50,000 vehicles a day.",
      "It estimates that more than fifty thousand vehicles cross the bridge every day."],
     "0,2",
     "「据估计」要用 It is estimated that ...（it 为形式主语）或 sth. is estimated to do；"
     "不能省略 that，也不能说 It estimates（缺少形式主语结构）。"),

    ("evaluate", "学校每学期都要对教师的教学效果进行评估。",
     ["The school evaluates teaching effectiveness every semester.",
      "The school evaluates teaching effective every semester.",
      "Teachers' teaching performance is assessed by the school each term.",
      "The school evaluates teaching effectiveness every semester for."],
     "0,2",
     "evaluate / assess 都表示「评估」；「效果」是名词 effectiveness，effective 是形容词。"
     "every semester 作状语，句尾不需再加介词。"),

    ("exceed", "今年的报名人数超过了三千。",
     ["The number of applicants this year exceeds three thousand.",
      "The number of applicants this year is exceeded three thousand.",
      "More than three thousand people have signed up this year.",
      "The number of applicants this year exceeds than three thousand."],
     "0,2",
     "exceed 是及物动词，直接接数量，不用被动、也不与 than 连用（than 只用于比较级）。"),

    ("expand", "学校计划明年扩大图书馆的阅览区。",
     ["The school plans to expand the reading area of the library next year.",
      "The school plans to expand the reading area of the library next year to.",
      "It is planned that the library's reading area will be enlarged next year.",
      "The school plans expanding the reading area of the library next year."],
     "0,2",
     "plan to do 是固定用法，不接动名词；expand / enlarge 均可表示「扩大」。"),

    ("explore", "科学家们仍在探索这一深海区域的奥秘。",
     ["Scientists are still exploring the mysteries of this deep-sea area.",
      "Scientists are still exploring the mysteries of this deep-sea area for.",
      "The secrets of this deep-sea region remain to be explored by scientists.",
      "Scientists are still exploring for the mysteries of this deep-sea area."],
     "0,2",
     "explore 作「探索」讲是及物动词，直接接宾语；explore for 表示「勘探（矿藏）」，语义不同。"),

    ("extend", "学校决定把借书期限延长到两个月。",
     ["The school has decided to extend the borrowing period to two months.",
      "The school has decided to extend the borrowing period for two months.",
      "It has been decided that books may be kept for two months instead.",
      "The school has decided to extend the borrowing period two months."],
     "0,2",
     "extend ... to ... 表示「延长到……」；「延长多久」用 for，但此处接的是终点值，应用 to。"
     "extend 后不能直接接双宾语。"),

    ("familiar", "我对这座城市的大街小巷都很熟悉。",
     ["I am quite familiar with the streets and alleys of this city.",
      "I am quite familiar to the streets and alleys of this city.",
      "The streets and lanes of the city are all familiar to me.",
      "I am quite familiar for the streets and alleys of this city."],
     "0,2",
     "sb. be familiar with sth.（人熟悉某物）；sth. be familiar to sb.（某物为人所熟悉）。"
     "两者的介词不可互换。"),

    ("favor", "大多数同学赞成把月考改为每两周一次。",
     ["Most students are in favour of changing the monthly test to once every two weeks.",
      "Most students are in favor to change the monthly test to once every two weeks.",
      "The majority of the class favour holding the monthly exam every other week.",
      "Most students are in favour for changing the monthly test to once every two weeks."],
     "0,2",
     "in favour of 后接名词或动名词；favour 作动词时可直接接动名词（favour doing）。"
     "英式拼写 favour 与美式拼写 favor 均可。"),
]
