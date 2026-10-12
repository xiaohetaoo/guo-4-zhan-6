# -*- coding: utf-8 -*-
"""六级真题句子翻译补充题（第 3 批）"""

STAGE = 2

ITEMS = [
    ("discrete", "整个问卷由若干个彼此独立的部分组成。",
     ["The questionnaire consists of several discrete parts.",
      "The questionnaire consists of several discreet parts.",
      "The survey is made up of a number of separate sections.",
      "The questionnaire consists of several discretion parts."],
     "0,2",
     "discrete 意为「分离的、各自独立的」，discreet 意为「谨慎的」，discretion 是名词「判断力」；"
     "separate 可作同义替换。"),

    ("displace", "电子书并没有完全取代纸质书的位置。",
     ["E-books have not entirely displaced paper books.",
      "E-books have not entirely replaced paper books of their position.",
      "Paper books have not been pushed out of the picture by electronic ones.",
      "E-books have not entirely displaced with paper books."],
     "0,2",
     "displace / replace 是及物动词，直接接宾语，不加 with，也不必再说 of their position；"
     "push sth. out of the picture 表示「把……挤出去」。"),

    ("diversify", "学校正在努力使选修课的种类更加多样化。",
     ["The school is trying to diversify the range of elective courses.",
      "The school is trying to diversify the range of elective courses more.",
      "Efforts are being made to make the elective courses more varied.",
      "The school is trying to diversiform the range of elective courses."],
     "0,2",
     "diversify 是动词「使多样化」；diversify 已含「变得多样」之意，再加 more 语义重复。"
     "形容词可用 varied / diverse。"),

    ("drastic", "过去十年，这个小区的居住条件发生了翻天覆地的变化。",
     ["Over the past decade, living conditions in this neighbourhood have undergone drastic changes.",
      "Over the past decade, living conditions in this neighbourhood have undergone drastic changes of.",
      "The past ten years have seen earth-shaking changes in housing conditions here.",
      "Over the past decade, living conditions in this neighbourhood have underdone drastic changes."],
     "0,2",
     "undergo 的过去分词是 undergone（underwent 是过去式），underdone 意为「未煮熟的」；"
     "drastic / earth-shaking changes 表示「巨大变化」。"),

    ("duplicate", "请把这份问卷复制两份交给另一位老师。",
     ["Please duplicate this questionnaire and give two copies to the other teacher.",
      "Please duplicate this questionnaire to two copies and give them to the other teacher.",
      "Two copies of the questionnaire should be made for the other teacher.",
      "Please duel this questionnaire and give two copies to the other teacher."],
     "0,2",
     "duplicate 是动词「复制」；duplicate ... to ... 不是正确搭配，应说 make two copies。"
     "duel 是「决斗」，与 duplicate 毫无关系。"),

    ("dwell", "他在演讲中反复谈到童年的乡村生活。",
     ["In his speech he dwelt on his childhood life in the countryside again and again.",
      "In his speech he dwelt in his childhood life in the countryside again and again.",
      "His talk kept returning to the years he spent as a child in the village.",
      "In his speech he dwelled on his childhood life in the countryside again and again."],
     "0,2",
     "dwell on 表示「详述、老是想着」，介词用 on；"
     "dwell 的过去式在正式语体中多用 dwelt（dwelled 亦可见，但 dwelt 更规范）。"),

    ("elicit", "老师的提问引发了同学们热烈的讨论。",
     ["The teacher's question elicited a lively discussion among the students.",
      "The teacher's question elicited a lively discussion among the students to.",
      "A heated discussion was provoked by the question the teacher raised.",
      "The teacher's question illicited a lively discussion among the students."],
     "0,2",
     "elicit 表示「引出、引发（回应）」，拼写为 e-lic-it（不是 illicit「非法的」）；"
     "provoke 可作近义替换。"),

    ("eligible", "所有在校生都有资格申请这项奖学金。",
     ["All students currently enrolled are eligible to apply for the scholarship.",
      "All students currently enrolled are eligible for apply for the scholarship.",
      "Every student on campus qualifies for this scholarship.",
      "All students currently enrolled are eligible applying for the scholarship."],
     "0,2",
     "eligible to do sth. 与 qualify for sth. 是标准结构；eligible for 后接名词，不接动名词，"
     "也不说 eligible applying。"),

    ("insert", "这段音频已被插入到课件之中。",
     ["The audio clip has been inserted in the courseware.",
      "The audio clip has been inserted to the courseware.",
      "The recording is now built into the teaching slides.",
      "The audio clip has inserted in the courseware."],
     "0,2",
     "insert sth. in / into sth. 表示「把……插入……」；音频是被插入的对象，要用被动 has been inserted，"
     "主动式 has inserted 缺少宾语。"),

    ("emission", "新政策鼓励企业减少温室气体排放。",
     ["The new policy encourages enterprises to reduce greenhouse gas emissions.",
      "The new policy encourages enterprises to reduce greenhouse gas emission.",
      "Under the new policy, firms are urged to cut down on their greenhouse gas output.",
      "The new policy encourages enterprises to reduce greenhouse gas emissive."],
     "0,2",
     "emission 作「排放物」讲常用复数 emissions；emissive 是形容词「发射的」，不能作宾语。"
     "cut down on 表示「减少」。"),

    ("comprise", "这门课程涵盖了语言学的主要分支。",
     ["This course comprises the main branches of linguistics.",
      "This course comprises of the main branches of linguistics.",
      "The major branches of linguistics are all covered in this course.",
      "This course is comprised the main branches of linguistics."],
     "0,2",
     "comprise 是及物动词，直接接宾语，不加 of（be comprised of 才带 of）；"
     "cover 可作同义替换。"),

    ("sanction", "多位专家对这一教学方法表示赞同。",
     ["A number of experts have sanctioned this teaching method.",
      "A number of experts have sanctioned to this teaching method.",
      "This approach to teaching has won the approval of many specialists.",
      "A number of experts have sanctioned with this teaching method."],
     "0,2",
     "sanction 作动词可表示「认可、批准」，是及物动词，直接接宾语；"
     "win the approval of sb. 是名词化同义表达。"),

    ("endow", "这所大学多年来获得了大量社会捐赠。",
     ["Over the years this university has received substantial endowments from society.",
      "Over the years this university has received substantial endowment from society.",
      "Large sums of money have been donated to the university by people from all walks of life.",
      "Over the years this university has received substantial endowments of society."],
     "0,2",
     "endowment 作「捐赠款」讲常用复数 endowments；「来自社会」用 from society，"
     "of society 会变成「社会的捐赠」，逻辑错误。"),

    ("enforce", "规章制度只有在严格执行时才有意义。",
     ["Rules and regulations only make sense when they are strictly enforced.",
      "Rules and regulations only make sense when they are strictly enforced with.",
      "Strict enforcement is what gives meaning to any set of rules.",
      "Rules and regulations only make sense when they strictly enforce."],
     "0,2",
     "「规章制度被严格执行」要用被动 be enforced；主动式 they enforce 缺少宾语，逻辑错误。"),

    ("enrich", "课外活动极大地丰富了学生的校园生活。",
     ["Extracurricular activities have greatly enriched students' campus life.",
      "Extracurricular activities have greatly enriched students' campus life with.",
      "Students' life on campus is made much richer by after-class activities.",
      "Extracurricular activities have greatly enrich students' campus life."],
     "0,2",
     "现在完成时结构是 have + 过去分词 enriched；动词原形 enrich 不能跟在 have 后面。"),

    ("enrol", "今年有近三千名新生入学。",
     ["Nearly three thousand new students enrolled this year.",
      "Nearly three thousand new students were enrolled this year.",
      "The enrolment of freshmen this year came close to three thousand.",
      "Nearly three thousand new students enrolled in this year."],
     "0,1",
     "enrol 表示「注册入学」，既可作不及物（students enrolled）也可作及物被动（were enrolled）；"
     "this year 作状语时前不加介词 in。名词形式是 enrolment / enrollment。"),

    ("entail", "完成这篇论文需要进行大量的实地调查。",
     ["Completing this thesis entails a great deal of field investigation.",
      "Completing this thesis entails to do a great deal of field investigation.",
      "A large amount of fieldwork is involved in finishing the dissertation.",
      "Completing this thesis entails with a great deal of field investigation."],
     "0,2",
     "entail 是及物动词，后接名词或动名词，不接不定式；"
     "be involved in 表示「涉及、需要」。"),

    ("foresee", "我们可以预见一个没有纸质教材的课堂。",
     ["We can foresee a classroom without any printed textbooks.",
      "We can foresee a classroom without any printed textbooks of.",
      "It is possible to imagine a class in which no paper textbook is used.",
      "We can foresee of a classroom without any printed textbooks."],
     "0,2",
     "foresee / imagine 是及物动词，直接接宾语，不加 of；"
     "「一个没有纸质教材的课堂」用 without 短语即可，句尾不必再加介词。"),

    ("ethical", "研究者必须遵守相关的伦理规范。",
     ["Researchers must observe the relevant ethical norms.",
      "Researchers must observe the relevant ethical norms to.",
      "Compliance with ethical standards is compulsory for anyone doing research.",
      "Researchers must observe the relevant ethnic norms."],
     "0,2",
     "ethical 意为「伦理的」，ethnic 意为「民族的」，两者形近义完全不同。"
     "observe / comply with norms 表示「遵守规范」。"),

    ("evoke", "这首老歌唤起了他对故乡的回忆。",
     ["This old song evoked memories of his hometown.",
      "This old song evoked memories to his hometown.",
      "Memories of his native place were stirred up by the old melody.",
      "This old song invoked memories of his hometown."],
     "0,2",
     "evoke 表示「唤起（记忆、情感）」；invoke 表示「援引、祈求」，语义不同。"
     "memories of 表示「对……的回忆」。"),

    ("exempt", "残疾学生可以免修体育课。",
     ["Students with disabilities may be exempted from PE classes.",
      "Students with disabilities may be exempted of PE classes.",
      "PE lessons can be waived for students who have a disability.",
      "Students with disabilities may be exempted to PE classes."],
     "0,2",
     "exempt sb. from sth. 表示「免除某人某事」，介词用 from；waive 表示「免除（要求）」。"),

    ("exert", "同伴的影响会对青少年的选择施加很大作用。",
     ["Peer influence exerts a strong effect on the choices teenagers make.",
      "Peer influence exerts a strong effect to the choices teenagers make.",
      "The choices of adolescents are strongly affected by the influence of their peers.",
      "Peer influence exerts a strong effect on the choices teenagers make with."],
     "0,2",
     "exert an effect on sth. 是固定搭配（也可用 have an effect on）；介词用 on，不用 to。"),

    ("exotic", "这个小镇上出现了不少异域风味的餐馆。",
     ["A number of restaurants serving exotic food have appeared in the small town.",
      "A number of restaurants serving exotic food have appeared in the small town to.",
      "The little town now boasts several eateries offering foreign flavours.",
      "A number of restaurants serving exotic foods have appeared in the small town."],
     "0,2",
     "exotic food 作不可数用法指「异域风味的食物」，加 s 强调「多种异国食品」，此处与 restaurants 呼应不强；"
     "appear in 后接地点，句尾不可再加 to。"),

    ("explicit", "老师对作业的要求讲得很明确。",
     ["The teacher was very explicit about the requirements for the assignment.",
      "The teacher was very explicit of the requirements for the assignment.",
      "What was required for the homework was stated in plain terms by the teacher.",
      "The teacher was very explicit with the requirements for the assignment."],
     "0,2",
     "be explicit about sth. 表示「对某事讲得明确」；in plain terms 表示「直白地」。"),

    ("exploit", "这家公司充分利用了当地的太阳能资源。",
     ["The company has fully exploited the local solar energy resources.",
      "The company has fully exploited the local solar energy resources of.",
      "Full use has been made of the solar resources available locally.",
      "The company has fully explored the local solar energy resources."],
     "0,2",
     "exploit 表示「开发利用（资源）」，make full use of 是同义表达；"
     "explore 是「勘探、探索」，与 exploit 不同。"),

    ("extract", "研究人员从这些样本中提取了有效成分。",
     ["The researchers extracted the effective components from these samples.",
      "The researchers extracted the effective components of these samples.",
      "Effective ingredients were obtained from the samples by the research team.",
      "The researchers extracted the effective components to these samples."],
     "0,2",
     "extract A from B 表示「从 B 中提取 A」，介词用 from；obtain ... from ... 是同义结构。"),
]
