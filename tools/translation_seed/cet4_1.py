# -*- coding: utf-8 -*-
"""四级真题句子翻译补充题（第 1 批）

每条格式：(keyword, 中文题干, [四个英文译文], 正确项下标串, 解析)
约定：四个选项里恰好两个是可接受译法（体现「一词多译」），
另两个分别对应「形近词 / 词性误用 / 搭配错误 / 中式直译」中的典型错误。
"""

STAGE = 1

ITEMS = [
    ("achieve", "只要坚持下去，你终会实现自己的目标。",
     ["As long as you keep going, you will eventually achieve your goal.",
      "As long as you keep going, you will eventually achieve to your goal.",
      "Keeping on, your goal will be achieved eventually.",
      "You will reach your goal in the end if you persist."],
     "0,3",
     "achieve 是及物动词，直接接宾语（achieve a goal），后面不能加 to；也可用 reach / attain one's goal。"
     "第三句让 your goal 当主语、Keeping on 悬空，属于中式直译。"),

    ("acquire", "孩子们在玩耍中自然而然地习得语言。",
     ["Children acquire language naturally through play.",
      "Children require language naturally through play.",
      "Children pick up language naturally through play.",
      "Children acquire language natural through play."],
     "0,2",
     "acquire 表示「习得、获得」，也可用 pick up（不经意间学会）。require 是「需要」，与 acquire 形近义不同；"
     "修饰动词要用副词 naturally，不能用形容词 natural。"),

    ("adjust", "新生需要一段时间来适应大学的学习节奏。",
     ["Freshmen need some time to adjust to the pace of study at university.",
      "Freshmen need some time to adjust with the pace of study at university.",
      "It takes freshmen a while to adapt themselves to the study rhythm of college.",
      "Freshmen need some time to adjusting the pace of study at university."],
     "0,2",
     "adjust to / adapt oneself to 都表示「适应」，介词固定用 to，不能用 with；"
     "need some time 后接不定式 to adjust，不是 to adjusting。"),

    ("admit", "他承认自己在复习计划上花的时间不够。",
     ["He admitted that he had not spent enough time on his review plan.",
      "He admitted to spend not enough time on his review plan.",
      "He confessed that he had not devoted enough time to his revision schedule.",
      "He admitted him not spending enough time on his review plan."],
     "0,2",
     "admit 后接that 从句或动名词（admit doing），不接不定式；也可用 confess that。"
     "「承认自己……」用反身代词 him 是错的，主语与宾语同指才用 himself，且 admit 不接宾补结构。"),

    ("adopt", "这所学校采用了一种新的方法来教英语写作。",
     ["The school adopted a new approach to teaching English writing.",
      "The school adopted a new approach of teaching English writing.",
      "A new method for teaching English writing was adopted by the school.",
      "The school adopted a new approach teaching English writing."],
     "0,2",
     "approach to (doing) sth. 是固定搭配，介词用 to，后接动名词；也可说 a method for/of doing sth."
     "直接说 approach teaching 漏了介词。"),

    ("afford", "并非每个家庭都能负担得起让孩子出国留学的费用。",
     ["Not every family can afford to send their children to study abroad.",
      "Not every family can afford sending their children to study abroad.",
      "Not every family can bear the cost of sending their children to study overseas.",
      "Not every family can afford to sending their children to study abroad."],
     "0,2",
     "afford 后接不定式（afford to do），不接动名词，也没有 afford to doing 的说法；"
     "换一种译法可用 bear / cover the cost of doing sth.。"),

    ("allocate", "学校把更多的经费拨给了图书馆和实验室。",
     ["The school allocated more funds to the library and the laboratories.",
      "The school allocated more funds for the library and the laboratories.",
      "More funds were assigned by the school to the library and labs.",
      "The school allocated more funds the library and the laboratories."],
     "0,2",
     "allocate sth. to sb./sth. 是固定搭配，介词用 to；assign 也可表示「拨给、分配」。"
     "第四句少了介词，allocate 不能直接接双宾语。"),

    ("analyze", "研究人员分析了近十年收集到的全部数据。",
     ["The researchers analyzed all the data collected over the past decade.",
      "The researchers analyzed all the data collecting over the past decade.",
      "All the data gathered in the last ten years were analyzed by the research team.",
      "The researchers analyzed all the datum collected over the past decade."],
     "0,2",
     "「被收集的数据」要用过去分词 collected / gathered 作后置定语（表被动完成），现在分词 collecting 表主动进行；"
     "data 是 datum 的复数形式，本身已表复数，不能再写成 datum。"),

    ("announce", "校方宣布今年的运动会将在下个月举行。",
     ["The school announced that this year's sports meet would be held next month.",
      "The school announced this year's sports meet will be held next month.",
      "It was announced by the school that the annual sports meeting would take place next month.",
      "The school announced that this year's sports meet would hold next month."],
     "0,2",
     "主句 announced 是过去时，从句时态要呼应，用 would 不用 will；"
     "「运动会」是被举行，要用被动 be held / take place（take place 无被动）。"),

    ("apply", "这套方法同样适用于英语听力训练。",
     ["This method also applies to English listening training.",
      "This method also applies for English listening training.",
      "The same method can be applied to the training of English listening.",
      "This method also applies in English listening training."],
     "0,2",
     "apply to 表示「适用于」，apply for 是「申请」，两者不可混用；介词也不能换成 in。"),

    ("appreciate", "我非常感激老师们这些年给予我的帮助。",
     ["I really appreciate the help my teachers have given me over the years.",
      "I really appreciate to the help my teachers have given me over the years.",
      "I am truly grateful for all the help my teachers have offered me these years.",
      "I really appreciate that my teachers have given me over the years."],
     "0,2",
     "appreciate 是及物动词，直接接名词（appreciate the help），不加介词 to；"
     "也可用 be grateful for sth.。第四句 appreciate 后接 that 从句却缺宾语，句子不完整。"),

    ("approach", "解决这一问题的最佳途径是加强国际合作。",
     ["The best approach to solving this problem is to strengthen international cooperation.",
      "The best approach of solving this problem is to strengthen international cooperation.",
      "Strengthening international cooperation is the best way to tackle this issue.",
      "The best approach solving this problem is to strengthen international cooperation."],
     "0,2",
     "approach to doing sth. 介词固定用 to；也可用 the best way to do / tackle the issue。"
     "第四句漏掉 to，成了 approach solving，语法错误。"),

    ("approve", "委员会已经批准了这项新的校园节能方案。",
     ["The committee has approved the new campus energy-saving plan.",
      "The committee has approved of the new campus energy-saving plan.",
      "The new energy-saving plan for the campus has been given official approval.",
      "The committee has approved to the new campus energy-saving plan."],
     "0,2",
     "approve 作「批准」讲是及物动词，直接接宾语；approve of 表示「赞成、认可」，语义偏弱。"
     "名词形式可用 give official approval to sth.。"),

    ("arise", "新问题往往在旧问题刚刚解决时出现。",
     ["New problems often arise just as the old ones are solved.",
      "New problems often rise just as the old ones are solved.",
      "New problems tend to emerge right after the old ones have been dealt with.",
      "New problems often arouse just as the old ones are solved."],
     "0,2",
     "arise 表示「出现、产生」，rise 是「上升」，arouse 是「激起（情绪）」，三者形近但义不同；"
     "也可用 emerge / come up 替换 arise。"),

    ("arrange", "老师为我们安排了一次与外国留学生的交流活动。",
     ["The teacher arranged an exchange activity with overseas students for us.",
      "The teacher arranged us an exchange activity with overseas students.",
      "An exchange activity with international students was arranged for us by our teacher.",
      "The teacher arranged for us an exchange activity with overseas students."],
     "0,2",
     "arrange 不能接双宾语，要说 arrange sth. for sb.；arrange for sb. to do 才接人。"
     "「安排某人做某事」不是 arrange sb. sth.。"),

    ("assume", "我们不能假定所有人都同意这个方案。",
     ["We cannot assume that everyone agrees with this plan.",
      "We cannot assume everyone to agree with this plan.",
      "It cannot be assumed that all the people are in favour of the proposal.",
      "We cannot assume of that everyone agrees with this plan."],
     "0,2",
     "assume 后接 that 从句，不接「宾语 + 不定式」结构（assume sb. to do 不成立），也不加介词 of；"
     "be in favour of 可替换 agree with。"),

    ("attach", "中国人历来十分重视家庭教育。",
     ["Chinese people have always attached great importance to family education.",
      "Chinese people have always attached great importance on family education.",
      "Great importance has always been attached to family education in China.",
      "Chinese people have always attached great importance with family education."],
     "0,2",
     "attach importance to sth. 是固定搭配，介词用 to，不能换成 on / with。"
     "被动式 Great importance has been attached to... 是常见正式写法。"),

    ("attempt", "他试图在三个月内读完二十本英文原著。",
     ["He attempted to read twenty original English books within three months.",
      "He attempted reading twenty original English books within three months.",
      "He made an attempt to finish twenty English originals in three months.",
      "He attempted to reading twenty original English books within three months."],
     "0,2",
     "attempt 后接不定式（attempt to do），名词结构是 make an attempt to do；"
     "attempt doing 与 attempt to doing 都不成立。"),

    ("attract", "这座城市每年吸引数以万计的游客。",
     ["The city attracts tens of thousands of tourists every year.",
      "The city attacks tens of thousands of tourists every year.",
      "Tens of thousands of visitors are drawn to the city each year.",
      "The city attracts tens of thousands of tourists every year to come."],
     "0,2",
     "attract「吸引」与 attack「攻击」仅一词之差；也可用 be drawn to（被吸引到）。"
     "attract sb. to come 冗赘，attract 本身已含「引来」之意。"),

    ("avoid", "为了避免犯错，他把每一步都写在了笔记本上。",
     ["To avoid making mistakes, he wrote down every step in his notebook.",
      "To avoid to make mistakes, he wrote down every step in his notebook.",
      "He noted down every step so as not to make any mistake.",
      "To avoid making mistakes, he wrote down every step in his notebook to."],
     "0,2",
     "avoid 后接动名词（avoid doing），不接不定式；也可用 so as not to do 表示「以免」。"
     "句尾多一个 to 使句子结构残缺。"),

    ("balance", "大学生应该学会平衡学习与社团活动。",
     ["College students should learn to balance study and club activities.",
      "College students should learn to balance study with club activities.",
      "College students ought to learn how to strike a balance between study and campus activities.",
      "College students should learn to balance between study and club activities."],
     "1,2",
     "balance A with B 或 balance A and B 都可以；也可用 strike a balance between A and B。"
     "balance between 缺少并列的两项，结构不完整。"),
]
