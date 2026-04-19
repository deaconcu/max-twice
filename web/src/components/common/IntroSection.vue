<template>
  <div class="intro-content" ref="scrollContainer">
    <!-- 第一屏：核心价值主张 -->
    <section class="intro-screen screen-1">
      <div class="screen-inner">
        <div class="tagline">学习平台</div>
        <h1 class="main-title">
          任何知识点<br />
          <span class="highlight">最多看两遍就懂</span>
        </h1>
        <p class="sub-text">同一个知识点，多种讲解方式，最易懂的排最前</p>
        <div class="scroll-indicator">
          <span>滑动探索</span>
          <div class="scroll-line"></div>
        </div>
      </div>
    </section>

    <!-- 第二屏：角色驱动 -->
    <section class="intro-screen screen-2">
      <div class="screen-inner">
        <div class="section-label">01 · 角色驱动</div>
        <h2 class="section-title">你在承担哪些角色？</h2>

        <div class="sketch-container sketch-roles">
          <svg class="sketch-svg" viewBox="0 0 320 160" preserveAspectRatio="xMidYMid meet">
            <defs>
              <filter id="sketchy-roles" x="-5%" y="-5%" width="110%" height="110%">
                <feTurbulence type="fractalNoise" baseFrequency="0.04" numOctaves="3" result="noise"/>
                <feDisplacementMap in="SourceGraphic" in2="noise" scale="1" xChannelSelector="R" yChannelSelector="G"/>
              </filter>
            </defs>

            <!-- 第一行 -->
            <g>
              <path d="M 10,8 Q 12,6 70,7 Q 74,8 72,10 L 73,34 Q 72,38 69,36 L 11,37 Q 7,36 9,33 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="41" y="26" text-anchor="middle" class="role-chip-text">程序员</text>
            </g>
            <g>
              <path d="M 84,8 Q 86,6 134,7 Q 138,8 136,10 L 137,34 Q 136,38 133,36 L 85,37 Q 81,36 83,33 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="110" y="26" text-anchor="middle" class="role-chip-text">父亲</text>
            </g>
            <g>
              <path d="M 148,8 Q 150,6 208,7 Q 212,8 210,10 L 211,34 Q 210,38 207,36 L 149,37 Q 145,36 147,33 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="179" y="26" text-anchor="middle" class="role-chip-text">投资者</text>
            </g>
            <g>
              <path d="M 222,8 Q 224,6 272,7 Q 276,8 274,10 L 275,34 Q 274,38 271,36 L 223,37 Q 219,36 221,33 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="248" y="26" text-anchor="middle" class="role-chip-text">儿子</text>
            </g>

            <!-- 第二行 -->
            <g>
              <path d="M 30,52 Q 32,50 80,51 Q 84,52 82,54 L 83,78 Q 82,82 79,80 L 31,81 Q 27,80 29,77 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="56" y="70" text-anchor="middle" class="role-chip-text">跑者</text>
            </g>
            <g>
              <path d="M 94,52 Q 96,50 144,51 Q 148,52 146,54 L 147,78 Q 146,82 143,80 L 95,81 Q 91,80 93,77 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="120" y="70" text-anchor="middle" class="role-chip-text">丈夫</text>
            </g>
            <g>
              <path d="M 158,52 Q 160,50 208,51 Q 212,52 210,54 L 211,78 Q 210,82 207,80 L 159,81 Q 155,80 157,77 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="184" y="70" text-anchor="middle" class="role-chip-text">厨师</text>
            </g>
            <g>
              <path d="M 222,52 Q 224,50 282,51 Q 286,52 284,54 L 285,78 Q 284,82 281,80 L 223,81 Q 219,80 221,77 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="253" y="70" text-anchor="middle" class="role-chip-text">读书人</text>
            </g>

            <!-- 第三行 -->
            <g>
              <path d="M 50,96 Q 52,94 120,95 Q 124,96 122,98 L 123,122 Q 122,126 119,124 L 51,125 Q 47,124 49,121 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="86" y="114" text-anchor="middle" class="role-chip-text">团队领导</text>
            </g>
            <g>
              <path d="M 134,96 Q 136,94 214,95 Q 218,96 216,98 L 217,122 Q 216,126 213,124 L 135,125 Q 131,124 133,121 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="175" y="114" text-anchor="middle" class="role-chip-text">摄影爱好者</text>
            </g>
            <g>
              <path d="M 228,96 Q 230,94 268,95 Q 272,96 270,98 L 271,122 Q 270,126 267,124 L 229,125 Q 225,124 227,121 Z"
                    fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy-roles)"/>
              <text x="249" y="114" text-anchor="middle" class="role-chip-text">...</text>
            </g>
          </svg>
        </div>

        <p class="section-hint">每个角色，都值得好好学习</p>
      </div>
    </section>

    <!-- 第三屏：学习路径 -->
    <section class="intro-screen screen-3">
      <div class="screen-inner screen-inner-flow">
        <div class="section-label">02 · 清晰路径</div>
        <h2 class="section-title">知道先学什么，再学什么</h2>

        <!-- 角色 Tab 切换 - 手绘风格 -->
        <div class="role-tabs-sketch">
          <svg class="role-tabs-svg" viewBox="0 0 200 36" preserveAspectRatio="xMidYMid meet">
            <defs>
              <filter id="sketchy-tab" x="-5%" y="-5%" width="110%" height="110%">
                <feTurbulence type="fractalNoise" baseFrequency="0.04" numOctaves="3" result="noise"/>
                <feDisplacementMap in="SourceGraphic" in2="noise" scale="1" xChannelSelector="R" yChannelSelector="G"/>
              </filter>
            </defs>
            <!-- 程序员 -->
            <g class="tab-item" @click="activeRoleIndex = 0">
              <rect x="0" y="0" width="64" height="36" fill="transparent" class="tab-hit-area"/>
              <path d="M 2,4 Q 4,2 58,3 Q 62,4 60,6 L 61,30 Q 60,34 57,32 L 3,33 Q -1,32 1,29 Z"
                    :fill="activeRoleIndex === 0 ? 'rgba(var(--v-theme-primary), 0.15)' : 'none'"
                    :stroke="activeRoleIndex === 0 ? 'rgb(var(--v-theme-primary))' : '#bbb'"
                    stroke-width="1.5" filter="url(#sketchy-tab)"/>
              <text x="31" y="22" text-anchor="middle"
                    :class="['tab-text', { active: activeRoleIndex === 0 }]">程序员</text>
            </g>
            <!-- 投资者 -->
            <g class="tab-item" @click="activeRoleIndex = 1">
              <rect x="68" y="0" width="64" height="36" fill="transparent" class="tab-hit-area"/>
              <path d="M 70,4 Q 72,2 128,3 Q 132,4 130,6 L 131,30 Q 130,34 127,32 L 71,33 Q 67,32 69,29 Z"
                    :fill="activeRoleIndex === 1 ? 'rgba(var(--v-theme-primary), 0.15)' : 'none'"
                    :stroke="activeRoleIndex === 1 ? 'rgb(var(--v-theme-primary))' : '#bbb'"
                    stroke-width="1.5" filter="url(#sketchy-tab)"/>
              <text x="100" y="22" text-anchor="middle"
                    :class="['tab-text', { active: activeRoleIndex === 1 }]">投资者</text>
            </g>
            <!-- 父亲 -->
            <g class="tab-item" @click="activeRoleIndex = 2">
              <rect x="136" y="0" width="64" height="36" fill="transparent" class="tab-hit-area"/>
              <path d="M 140,4 Q 142,2 196,3 Q 200,4 198,6 L 199,30 Q 198,34 195,32 L 141,33 Q 137,32 139,29 Z"
                    :fill="activeRoleIndex === 2 ? 'rgba(var(--v-theme-primary), 0.15)' : 'none'"
                    :stroke="activeRoleIndex === 2 ? 'rgb(var(--v-theme-primary))' : '#bbb'"
                    stroke-width="1.5" filter="url(#sketchy-tab)"/>
              <text x="169" y="22" text-anchor="middle"
                    :class="['tab-text', { active: activeRoleIndex === 2 }]">父亲</text>
            </g>
          </svg>
        </div>

        <div class="sketch-container sketch-flow">
          <svg class="sketch-svg" viewBox="0 0 500 200" preserveAspectRatio="xMidYMid meet">
            <!-- 手绘风格滤镜 -->
            <defs>
              <filter id="sketchy-flow" x="-5%" y="-5%" width="110%" height="110%">
                <feTurbulence type="fractalNoise" baseFrequency="0.03" numOctaves="3" result="noise"/>
                <feDisplacementMap in="SourceGraphic" in2="noise" scale="1.5" xChannelSelector="R" yChannelSelector="G"/>
              </filter>
            </defs>

            <!-- 程序员路径 -->
            <g v-if="activeRoleIndex === 0">
              <!-- 节点：决策树 (done) -->
              <rect x="20" y="30" width="70" height="32" rx="6" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="55" y="51" text-anchor="middle" class="sketch-node-text">决策树</text>

              <!-- 节点：Flask基础 (done) -->
              <rect x="20" y="120" width="70" height="32" rx="6" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="55" y="141" text-anchor="middle" class="sketch-node-text">Flask基础</text>

              <!-- 节点：机器学习 (done) -->
              <rect x="140" y="10" width="70" height="32" rx="6" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="175" y="31" text-anchor="middle" class="sketch-node-text">机器学习</text>

              <!-- 节点：深度学习 (active) -->
              <rect x="140" y="55" width="70" height="32" rx="6" fill="none" stroke="#42a5f5" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="175" y="76" text-anchor="middle" class="sketch-node-text">深度学习</text>

              <!-- 节点：Web开发 (done) -->
              <rect x="140" y="100" width="70" height="32" rx="6" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="175" y="121" text-anchor="middle" class="sketch-node-text">Web开发</text>

              <!-- 节点：系统设计 (active) -->
              <rect x="140" y="145" width="70" height="32" rx="6" fill="none" stroke="#42a5f5" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="175" y="166" text-anchor="middle" class="sketch-node-text">系统设计</text>

              <!-- 节点：AI路线 (locked) -->
              <rect x="260" y="30" width="70" height="32" rx="6" fill="none" stroke="#e0e0e0" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="295" y="51" text-anchor="middle" class="sketch-node-text locked">AI路线</text>

              <!-- 节点：后端路线 (locked) -->
              <rect x="260" y="120" width="70" height="32" rx="6" fill="none" stroke="#e0e0e0" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="295" y="141" text-anchor="middle" class="sketch-node-text locked">后端路线</text>

              <!-- 节点：程序员 (role) -->
              <rect x="380" y="75" width="80" height="36" rx="8" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="2" filter="url(#sketchy-flow)"/>
              <text x="420" y="98" text-anchor="middle" class="sketch-node-text role-outline">程序员</text>

              <!-- 连接线 - 起点终点都留8px间距，多条线错开 -->
              <!-- 决策树(90,46) → 机器学习(140,26) -->
              <path d="M 98,42 Q 118,30 132,26" fill="none" stroke="#66bb6a" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 决策树(90,46) → 深度学习(140,71) -->
              <path d="M 98,50 Q 118,64 132,71" fill="none" stroke="#42a5f5" stroke-width="2" stroke-dasharray="4,2" filter="url(#sketchy-flow)"/>
              <!-- Flask基础(90,136) → Web开发(140,116) -->
              <path d="M 98,132 Q 118,122 132,116" fill="none" stroke="#66bb6a" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- Flask基础(90,136) → 系统设计(140,161) -->
              <path d="M 98,140 Q 118,154 132,161" fill="none" stroke="#42a5f5" stroke-width="2" stroke-dasharray="4,2" filter="url(#sketchy-flow)"/>
              <!-- 机器学习(210,26) → AI路线(260,46) -->
              <path d="M 218,26 Q 238,32 252,40" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 深度学习(210,71) → AI路线(260,46) -->
              <path d="M 218,71 Q 238,60 252,52" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- Web开发(210,116) → 后端路线(260,136) -->
              <path d="M 218,116 Q 238,122 252,130" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 系统设计(210,161) → 后端路线(260,136) -->
              <path d="M 218,161 Q 238,150 252,142" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- AI路线(330,46) → 程序员(380,93) -->
              <path d="M 338,46 Q 358,64 372,86" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 后端路线(330,136) → 程序员(380,93) -->
              <path d="M 338,136 Q 358,116 372,100" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
            </g>

            <!-- 投资者路径 -->
            <g v-if="activeRoleIndex === 1">
              <!-- 节点：财务报表 (done) -->
              <rect x="20" y="50" width="70" height="32" rx="6" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="55" y="71" text-anchor="middle" class="sketch-node-text">财务报表</text>

              <!-- 节点：估值方法 (active) -->
              <rect x="20" y="110" width="70" height="32" rx="6" fill="none" stroke="#42a5f5" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="55" y="131" text-anchor="middle" class="sketch-node-text">估值方法</text>

              <!-- 节点：价值投资 (done) -->
              <rect x="150" y="30" width="70" height="32" rx="6" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="185" y="51" text-anchor="middle" class="sketch-node-text">价值投资</text>

              <!-- 节点：行业分析 (active) -->
              <rect x="150" y="80" width="70" height="32" rx="6" fill="none" stroke="#42a5f5" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="185" y="101" text-anchor="middle" class="sketch-node-text">行业分析</text>

              <!-- 节点：风险管理 (locked) -->
              <rect x="150" y="130" width="70" height="32" rx="6" fill="none" stroke="#e0e0e0" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="185" y="151" text-anchor="middle" class="sketch-node-text locked">风险管理</text>

              <!-- 节点：长期投资路线 (locked) -->
              <rect x="280" y="80" width="80" height="32" rx="6" fill="none" stroke="#e0e0e0" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="320" y="101" text-anchor="middle" class="sketch-node-text locked">长期投资</text>

              <!-- 节点：投资者 (role) -->
              <rect x="400" y="74" width="70" height="36" rx="8" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="2" filter="url(#sketchy-flow)"/>
              <text x="435" y="97" text-anchor="middle" class="sketch-node-text role-outline">投资者</text>

              <!-- 连接线 - 起点终点都留8px间距，多条线错开 -->
              <!-- 财务报表(90,66) → 价值投资(150,46) -->
              <path d="M 98,62 Q 124,50 142,46" fill="none" stroke="#66bb6a" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 财务报表(90,66) → 行业分析(150,96) -->
              <path d="M 98,70 Q 124,86 142,90" fill="none" stroke="#42a5f5" stroke-width="2" stroke-dasharray="4,2" filter="url(#sketchy-flow)"/>
              <!-- 估值方法(90,126) → 行业分析(150,96) -->
              <path d="M 98,122 Q 124,108 142,102" fill="none" stroke="#42a5f5" stroke-width="2" stroke-dasharray="4,2" filter="url(#sketchy-flow)"/>
              <!-- 估值方法(90,126) → 风险管理(150,146) -->
              <path d="M 98,130 Q 124,142 142,146" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 价值投资(220,46) → 长期投资(280,96) -->
              <path d="M 228,46 Q 254,66 272,88" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 行业分析(220,96) → 长期投资(280,96) -->
              <path d="M 228,96 Q 250,91 272,96" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 风险管理(220,146) → 长期投资(280,96) -->
              <path d="M 228,146 Q 254,124 272,104" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 长期投资(360,96) → 投资者(400,92) -->
              <path d="M 368,96 Q 384,94 392,92" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
            </g>

            <!-- 父亲路径 -->
            <g v-if="activeRoleIndex === 2">
              <!-- 节点：儿童心理 (done) -->
              <rect x="20" y="50" width="70" height="32" rx="6" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="55" y="71" text-anchor="middle" class="sketch-node-text">儿童心理</text>

              <!-- 节点：沟通技巧 (done) -->
              <rect x="20" y="110" width="70" height="32" rx="6" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="55" y="131" text-anchor="middle" class="sketch-node-text">沟通技巧</text>

              <!-- 节点：亲子教育 (done) -->
              <rect x="150" y="30" width="70" height="32" rx="6" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="185" y="51" text-anchor="middle" class="sketch-node-text">亲子教育</text>

              <!-- 节点：情绪管理 (active) -->
              <rect x="150" y="80" width="70" height="32" rx="6" fill="none" stroke="#42a5f5" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="185" y="101" text-anchor="middle" class="sketch-node-text">情绪管理</text>

              <!-- 节点：学习辅导 (locked) -->
              <rect x="150" y="130" width="70" height="32" rx="6" fill="none" stroke="#e0e0e0" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="185" y="151" text-anchor="middle" class="sketch-node-text locked">学习辅导</text>

              <!-- 节点：好爸爸路线 (locked) -->
              <rect x="280" y="80" width="80" height="32" rx="6" fill="none" stroke="#e0e0e0" stroke-width="1.5" filter="url(#sketchy-flow)"/>
              <text x="320" y="101" text-anchor="middle" class="sketch-node-text locked">好爸爸路线</text>

              <!-- 节点：父亲 (role) -->
              <rect x="400" y="74" width="70" height="36" rx="8" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="2" filter="url(#sketchy-flow)"/>
              <text x="435" y="97" text-anchor="middle" class="sketch-node-text role-outline">父亲</text>

              <!-- 连接线 - 起点终点都留8px间距，多条线错开 -->
              <!-- 儿童心理(90,66) → 亲子教育(150,46) -->
              <path d="M 98,62 Q 124,48 142,44" fill="none" stroke="#66bb6a" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 儿童心理(90,66) → 情绪管理(150,96) -->
              <path d="M 98,70 Q 124,86 142,90" fill="none" stroke="#42a5f5" stroke-width="2" stroke-dasharray="4,2" filter="url(#sketchy-flow)"/>
              <!-- 沟通技巧(90,126) → 亲子教育(150,46) -->
              <path d="M 98,118 Q 124,70 142,52" fill="none" stroke="#66bb6a" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 沟通技巧(90,126) → 情绪管理(150,96) -->
              <path d="M 98,126 Q 124,108 142,102" fill="none" stroke="#42a5f5" stroke-width="2" stroke-dasharray="4,2" filter="url(#sketchy-flow)"/>
              <!-- 沟通技巧(90,126) → 学习辅导(150,146) -->
              <path d="M 98,134 Q 124,144 142,146" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 亲子教育(220,46) → 好爸爸路线(280,96) -->
              <path d="M 228,46 Q 254,66 272,88" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 情绪管理(220,96) → 好爸爸路线(280,96) -->
              <path d="M 228,96 Q 250,91 272,96" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 学习辅导(220,146) → 好爸爸路线(280,96) -->
              <path d="M 228,146 Q 254,124 272,104" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
              <!-- 好爸爸路线(360,96) → 父亲(400,92) -->
              <path d="M 368,96 Q 384,94 392,92" fill="none" stroke="#e0e0e0" stroke-width="2" filter="url(#sketchy-flow)"/>
            </g>
          </svg>
        </div>
        <div class="tree-legend">
          <span class="legend-item"><span class="legend-dot done"></span>已完成</span>
          <span class="legend-item"><span class="legend-dot active"></span>正在学</span>
          <span class="legend-item"><span class="legend-dot locked"></span>待解锁</span>
        </div>
        <p class="section-hint">社区贡献路径，持续优化</p>
      </div>
    </section>

    <!-- 第四屏：课程阅读 -->
    <section class="intro-screen screen-4">
      <div class="screen-inner screen-inner-full">
        <div class="section-label">03 · 课程阅读</div>
        <h2 class="section-title">同一知识点，最易懂的排最前</h2>

        <div class="sketch-container">
          <svg class="sketch-svg" viewBox="-100 0 560 300" preserveAspectRatio="xMidYMid meet">
            <!-- 手绘风格滤镜 -->
            <defs>
              <filter id="sketchy" x="-5%" y="-5%" width="110%" height="110%">
                <feTurbulence type="fractalNoise" baseFrequency="0.03" numOctaves="3" result="noise"/>
                <feDisplacementMap in="SourceGraphic" in2="noise" scale="1.5" xChannelSelector="R" yChannelSelector="G"/>
              </filter>
            </defs>

            <!-- 左侧目录框 - 堆叠效果表示多个目录组 -->
            <!-- 底层目录（第三层） -->
            <path d="M 30,40 Q 32,38 126,39 Q 130,40 128,41 L 129,216 Q 128,220 126,218 L 31,219 Q 27,218 28,215 Z"
                  fill="#f5f5f5" stroke="#ccc" stroke-width="1" filter="url(#sketchy)"/>
            <!-- 中层目录（第二层） -->
            <path d="M 26,36 Q 28,34 122,35 Q 126,36 124,37 L 125,212 Q 124,216 122,214 L 27,215 Q 23,214 24,211 Z"
                  fill="#fafafa" stroke="#bbb" stroke-width="1" filter="url(#sketchy)"/>
            <!-- 顶层目录（当前选中） -->
            <path d="M 22,32 Q 24,30 118,31 Q 122,32 120,33 L 121,208 Q 120,212 118,210 L 23,211 Q 19,210 20,207 Z"
                  fill="#fff" stroke="#555" stroke-width="1.5" filter="url(#sketchy)"/>
            <text x="70" y="22" text-anchor="middle" class="sketch-label-hand">目录</text>

            <!-- 目录项 - 手绘高亮 -->
            <path d="M 28,46 Q 30,44 108,45 Q 112,46 110,48 L 111,66 Q 110,70 108,68 L 29,69 Q 25,68 27,65 Z"
                  fill="rgba(var(--v-theme-primary), 0.12)" stroke="rgb(var(--v-theme-primary))" stroke-width="1.2" filter="url(#sketchy)"/>
            <text x="70" y="61" text-anchor="middle" class="sketch-item-hand">决策树原理</text>

            <!-- 其他目录项 - 手绘线条 -->
            <path d="M 32,82 Q 50,81 80,83 Q 100,82 106,83" fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy)"/>
            <path d="M 32,100 Q 55,99 75,101 Q 90,100 98,101" fill="none" stroke="#bbb" stroke-width="1.2" filter="url(#sketchy)"/>
            <path d="M 32,118 Q 48,117 68,119 Q 82,118 92,119" fill="none" stroke="#ccc" stroke-width="1.2" filter="url(#sketchy)"/>
            <path d="M 32,136 Q 45,135 60,137 Q 75,136 85,137" fill="none" stroke="#ddd" stroke-width="1.2" filter="url(#sketchy)"/>

            <!-- 右侧内容框 - 手绘风格 -->
            <path d="M 142,32 Q 145,29 338,31 Q 342,33 340,35 L 341,208 Q 340,212 337,210 L 143,211 Q 139,209 141,206 Z"
                  fill="none" stroke="#555" stroke-width="1.5" filter="url(#sketchy)"/>
            <text x="240" y="22" text-anchor="middle" class="sketch-label-hand">文章列表</text>

            <!-- 文章排名 - 手绘卡片 -->
            <g class="article-row">
              <text x="160" y="62" class="sketch-rank">1</text>
              <path d="M 176,44 Q 180,42 322,43 Q 326,45 324,47 L 325,70 Q 324,74 321,72 L 177,73 Q 173,71 175,68 Z"
                    fill="rgba(255,215,0,0.12)" stroke="#e6c200" stroke-width="1.2" filter="url(#sketchy)"/>
              <!-- 文章内容用线条表示 -->
              <path d="M 185,55 Q 200,54 240,56 Q 280,55 310,56" fill="none" stroke="#999" stroke-width="1.2" filter="url(#sketchy)"/>
              <path d="M 185,65 Q 210,64 250,66 Q 270,65 295,66" fill="none" stroke="#bbb" stroke-width="1" filter="url(#sketchy)"/>
            </g>

            <g class="article-row">
              <text x="160" y="102" class="sketch-rank">2</text>
              <path d="M 176,84 Q 179,82 322,83 Q 326,85 324,87 L 325,110 Q 324,114 321,112 L 177,113 Q 173,111 175,108 Z"
                    fill="#fafafa" stroke="#ccc" stroke-width="1.2" filter="url(#sketchy)"/>
              <path d="M 185,95 Q 205,94 245,96 Q 285,95 315,96" fill="none" stroke="#999" stroke-width="1.2" filter="url(#sketchy)"/>
              <path d="M 185,105 Q 215,104 255,106 Q 275,105 300,106" fill="none" stroke="#bbb" stroke-width="1" filter="url(#sketchy)"/>
            </g>

            <g class="article-row">
              <text x="160" y="142" class="sketch-rank">3</text>
              <path d="M 176,124 Q 180,122 322,123 Q 326,125 324,127 L 325,150 Q 324,154 321,152 L 177,153 Q 173,151 175,148 Z"
                    fill="#fafafa" stroke="#ccc" stroke-width="1.2" filter="url(#sketchy)"/>
              <path d="M 185,135 Q 200,134 235,136 Q 275,135 305,136" fill="none" stroke="#999" stroke-width="1.2" filter="url(#sketchy)"/>
              <path d="M 185,145 Q 220,144 260,146 Q 280,145 290,146" fill="none" stroke="#bbb" stroke-width="1" filter="url(#sketchy)"/>
            </g>

            <!-- 省略号 -->
            <text x="250" y="178" text-anchor="middle" fill="#999" class="sketch-dots">· · ·</text>

            <!-- 左侧标注 - 手绘曲线箭头 -->
            <path d="M 30,110 C 10,110 -5,85 -20,65" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" filter="url(#sketchy)"/>
            <path d="M -20,74 L -20,65 L -11,65" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#sketchy)"/>
            <text x="-30" y="40" text-anchor="middle" class="sketch-annotation-hand">
              <tspan x="-30" dy="0">社区贡献节点</tspan>
              <tspan x="-30" dy="16">自选组成目录</tspan>
            </text>

            <!-- 堆叠目录标注 - 指向目录框右上角的堆叠部分 -->
            <path d="M 18,180 C -5,180 -15,200 -25,215" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" filter="url(#sketchy)"/>
            <path d="M -31,206 L -25,215 L -17,212" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#sketchy)"/>
            <text x="-35" y="235" text-anchor="middle" class="sketch-annotation-hand">
              <tspan x="-35" dy="0">多个目录组</tspan>
              <tspan x="-35" dy="16">支持不同学习路径</tspan>
            </text>

            <!-- 右侧标注 - 手绘曲线箭头，从第一篇文章开始 -->
            <path d="M 310,75 C 330,85 360,115 380,135" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" filter="url(#sketchy)"/>
            <path d="M 371,135 L 380,135 L 380,126" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#sketchy)"/>
            <text x="400" y="155" text-anchor="middle" class="sketch-annotation-hand">
              <tspan x="400" dy="0">用户投票</tspan>
              <tspan x="400" dy="16">易懂文章优先展示</tspan>
            </text>

            <!-- 底部标注 - 从省略号指向文字（箭头朝下） -->
            <path d="M 240,190 C 238,200 242,210 240,230" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" filter="url(#sketchy)"/>
            <path d="M 234,223 L 240,230 L 246,223" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" filter="url(#sketchy)"/>
            <text x="240" y="255" text-anchor="middle" class="sketch-annotation-hand">
              第一篇不懂？往下看，总有适合你的
            </text>
          </svg>
        </div>

        <p class="section-hint">好内容自然上浮，学习更高效</p>
      </div>
    </section>

    <!-- 第五屏：科学复习 -->
    <section class="intro-screen screen-5">
      <div class="screen-inner screen-inner-full">
        <div class="section-label">04 · 科学复习</div>
        <h2 class="section-title">学完不忘，真正掌握</h2>

        <div class="sketch-container sketch-review">
          <svg class="sketch-svg" viewBox="0 0 300 190" preserveAspectRatio="xMidYMid meet">
            <!-- 手绘风格滤镜 -->
            <defs>
              <filter id="sketchy-review" x="-5%" y="-5%" width="110%" height="110%">
                <feTurbulence type="fractalNoise" baseFrequency="0.03" numOctaves="3" result="noise"/>
                <feDisplacementMap in="SourceGraphic" in2="noise" scale="1.5" xChannelSelector="R" yChannelSelector="G"/>
              </filter>
            </defs>

            <!-- 顶部统计行 -->
            <g class="review-stats-sketch">
              <text x="75" y="16" text-anchor="middle" class="sketch-stat-inline">今日待复习 <tspan class="primary" font-weight="700">12</tspan></text>
              <text x="150" y="16" text-anchor="middle" class="sketch-stat-inline">·</text>
              <text x="225" y="16" text-anchor="middle" class="sketch-stat-inline">连续打卡 <tspan font-weight="700">7</tspan> 天</text>
            </g>

            <!-- 记忆卡片 - 手绘风格 -->
            <g class="flashcard-sketch">
              <!-- 卡片背景 -->
              <path d="M 20,28 Q 22,26 278,27 Q 282,28 280,30 L 281,128 Q 280,132 277,130 L 21,131 Q 17,130 19,127 Z"
                    fill="rgba(var(--v-theme-primary), 0.08)" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" filter="url(#sketchy-review)"/>

              <!-- 问题图标 -->
              <circle cx="42" cy="55" r="12" fill="none" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" filter="url(#sketchy-review)"/>
              <text x="42" y="59" text-anchor="middle" class="sketch-icon-text">?</text>

              <!-- 问题标签 -->
              <text x="62" y="49" class="sketch-card-label">问题</text>

              <!-- 问题内容 - 手绘线条 -->
              <path d="M 62,59 Q 100,58 150,60 Q 200,59 260,60" fill="none" stroke="rgba(var(--v-theme-on-surface), 0.45)" stroke-width="1.5" filter="url(#sketchy-review)"/>

              <!-- 分隔虚线 -->
              <path d="M 32,78 Q 150,77 268,78" fill="none" stroke="rgba(var(--v-theme-on-surface), 0.15)" stroke-width="1" stroke-dasharray="5,3" filter="url(#sketchy-review)"/>

              <!-- 答案图标 -->
              <circle cx="42" cy="106" r="12" fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-review)"/>
              <text x="42" y="110" text-anchor="middle" class="sketch-icon-text answer">✓</text>

              <!-- 答案标签 -->
              <text x="62" y="100" class="sketch-card-label answer">答案</text>

              <!-- 答案内容 - 手绘线条 -->
              <path d="M 62,110 Q 95,109 140,111 Q 175,110 220,111" fill="none" stroke="rgba(var(--v-theme-on-surface), 0.35)" stroke-width="1.5" filter="url(#sketchy-review)"/>
            </g>

            <!-- 评价按钮区域 -->
            <g class="rating-buttons">
              <!-- 忘记按钮 -->
              <path d="M 20,142 Q 22,140 70,141 Q 74,142 72,144 L 73,168 Q 72,172 69,170 L 21,171 Q 17,170 19,167 Z"
                    fill="none" stroke="#ef5350" stroke-width="1.5" filter="url(#sketchy-review)"/>
              <text x="46" y="160" text-anchor="middle" class="sketch-btn-text forgot">忘记</text>

              <!-- 困难按钮 -->
              <path d="M 82,142 Q 84,140 132,141 Q 136,142 134,144 L 135,168 Q 134,172 131,170 L 83,171 Q 79,170 81,167 Z"
                    fill="none" stroke="#ffa726" stroke-width="1.5" filter="url(#sketchy-review)"/>
              <text x="108" y="160" text-anchor="middle" class="sketch-btn-text hard">困难</text>

              <!-- 良好按钮 -->
              <path d="M 144,142 Q 146,140 194,141 Q 198,142 196,144 L 197,168 Q 196,172 193,170 L 145,171 Q 141,170 143,167 Z"
                    fill="none" stroke="#66bb6a" stroke-width="1.5" filter="url(#sketchy-review)"/>
              <text x="170" y="160" text-anchor="middle" class="sketch-btn-text good">良好</text>

              <!-- 简单按钮 -->
              <path d="M 206,142 Q 208,140 278,141 Q 282,142 280,144 L 281,168 Q 280,172 277,170 L 207,171 Q 203,170 205,167 Z"
                    fill="rgba(var(--v-theme-primary), 0.12)" stroke="rgb(var(--v-theme-primary))" stroke-width="1.5" filter="url(#sketchy-review)"/>
              <text x="243" y="160" text-anchor="middle" class="sketch-btn-text easy">简单</text>
            </g>
          </svg>
        </div>

        <p class="section-hint">艾宾浩斯曲线，智能安排复习</p>
      </div>
    </section>

    <!-- 第六屏：欢迎加入 -->
    <section class="intro-screen screen-6">
      <div class="screen-inner screen-inner-welcome">
        <img :src="frontImage" alt="开始学习之旅" class="front-image" />
        <h2 class="welcome-title">准备好了吗？</h2>
        <p class="welcome-subtitle">快速学习，帮助你胜任多重角色</p>
        <p class="welcome-cta">登录，开启你的多面人生</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import frontImage from '@/assets/images/front.png'

const scrollContainer = ref<HTMLElement | null>(null)
const activeRoleIndex = ref(0)

// 角色数据
const roles = [
  { id: 'programmer', name: '程序员' },
  { id: 'investor', name: '投资者' },
  { id: 'father', name: '父亲' },
]
</script>

<style scoped>
.intro-content {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  scroll-snap-type: y proximity;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.intro-content::-webkit-scrollbar {
  display: none;
}

.intro-screen {
  height: 100%;
  min-height: 100%;
  scroll-snap-align: start;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
}

.screen-inner {
  width: 100%;
  max-width: 400px;
  text-align: center;
}

/* ========== 第一屏：核心价值 ========== */

.tagline {
  display: inline-block;
  font-size: 0.85rem;
  font-weight: 600;
  color: rgb(var(--v-theme-primary));
  background: rgba(var(--v-theme-primary), 0.1);
  padding: 6px 16px;
  border-radius: 20px;
  margin-bottom: 24px;
  letter-spacing: 2px;
}

.main-title {
  font-size: 2.2rem;
  font-weight: 800;
  line-height: 1.3;
  color: rgb(var(--v-theme-on-surface));
  margin: 0 0 20px 0;
}

.main-title .highlight {
  color: rgb(var(--v-theme-primary));
}

.sub-text {
  font-size: 1.1rem;
  color: rgba(var(--v-theme-on-surface), 0.6);
  margin: 0 0 48px 0;
  line-height: 1.5;
}

.scroll-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: rgba(var(--v-theme-on-surface), 0.35);
  font-size: 0.9rem;
}

.scroll-line {
  width: 1px;
  height: 40px;
  background: linear-gradient(180deg, rgba(var(--v-theme-on-surface), 0.3) 0%, transparent 100%);
  animation: scrollPulse 2s ease-in-out infinite;
}

@keyframes scrollPulse {
  0%, 100% { opacity: 0.3; transform: scaleY(1); }
  50% { opacity: 0.6; transform: scaleY(1.1); }
}

/* ========== 通用区块样式 ========== */
.section-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: rgb(var(--v-theme-primary));
  letter-spacing: 1px;
  margin-bottom: 10px;
  text-transform: uppercase;
}

.section-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
  margin: 0 0 28px 0;
}

.section-hint {
  font-size: 0.95rem;
  color: rgba(var(--v-theme-on-surface), 0.5);
  margin: 48px 0 0 0;
}

/* ========== 第二屏：角色（手绘风格） ========== */
.sketch-roles {
  margin-bottom: 24px;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
}

.role-chip-text {
  font-size: 11px;
  font-weight: 500;
  fill: rgba(var(--v-theme-on-surface), 0.7);
}

/* ========== 第三屏：手绘路径 ========== */
.screen-inner-flow {
  max-width: 600px;
}

/* 角色 Tab 切换 - 手绘风格 */
.role-tabs-sketch {
  display: flex;
  justify-content: center;
  margin-bottom: 36px;
}

.role-tabs-svg {
  width: 220px;
  height: 40px;
}

.tab-item {
  cursor: pointer;
}

.tab-hit-area {
  cursor: pointer;
}

.tab-text {
  font-size: 11px;
  font-weight: 500;
  fill: rgba(var(--v-theme-on-surface), 0.6);
  pointer-events: none;
}

.tab-text.active {
  fill: rgb(var(--v-theme-primary));
  font-weight: 600;
}

.sketch-flow {
  height: 220px;
  margin-bottom: 24px;
}

/* 手绘节点文字 */
.sketch-node-text {
  font-size: 11px;
  font-weight: 500;
  fill: #333;
}

.sketch-node-text.locked {
  fill: #9e9e9e;
}

.sketch-node-text.role {
  fill: white;
  font-weight: 600;
}

.sketch-node-text.role-outline {
  fill: rgb(var(--v-theme-primary));
  font-weight: 600;
}

/* ========== 第四屏：手绘风格课程图示 ========== */
.screen-inner-full {
  max-width: 800px;
}

.sketch-container {
  width: 100%;
}

.sketch-svg {
  width: 100%;
  height: auto;
}

.sketch-label-hand {
  font-size: 12px;
  fill: #666;
  font-weight: 500;
}

.sketch-item-hand {
  font-size: 11px;
  fill: rgb(var(--v-theme-primary));
  font-weight: 500;
}

.sketch-medal {
  font-size: 14px;
}

.sketch-rank {
  font-size: 11px;
  font-weight: 600;
  fill: #666;
}

.sketch-article-hand {
  font-size: 11px;
  fill: #444;
}

.sketch-dots {
  font-size: 16px;
  letter-spacing: 4px;
}

.sketch-annotation-hand {
  font-size: 12px;
  fill: rgba(var(--v-theme-on-surface), 0.75);
  font-weight: 500;
}

/* 图例 */
.tree-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: rgba(var(--v-theme-on-surface), 0.6);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: rgba(var(--v-theme-surface-variant), 0.6);
  border: 2px solid rgba(var(--v-theme-on-surface), 0.2);
}

.legend-dot.done {
  background: #e8f5e9;
  border-color: #66bb6a;
}

.legend-dot.active {
  background: #e3f2fd;
  border-color: #42a5f5;
}

.legend-dot.locked {
  background: #f5f5f5;
  border-color: #e0e0e0;
}

/* ========== 第五屏：复习（手绘风格） ========== */
.sketch-review {
  margin-bottom: 24px;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

.sketch-stat-inline {
  font-size: 10px;
  fill: rgba(var(--v-theme-on-surface), 0.6);
}

.sketch-stat-inline .primary {
  fill: rgb(var(--v-theme-primary));
}

.sketch-icon-text {
  font-size: 16px;
  font-weight: 700;
  fill: rgb(var(--v-theme-primary));
}

.sketch-icon-text.answer {
  fill: #66bb6a;
}

.sketch-card-label {
  font-size: 11px;
  font-weight: 600;
  fill: rgb(var(--v-theme-primary));
}

.sketch-card-label.answer {
  fill: #66bb6a;
}

.sketch-btn-text {
  font-size: 11px;
  font-weight: 500;
}

.sketch-btn-text.forgot {
  fill: #ef5350;
}

.sketch-btn-text.hard {
  fill: #ffa726;
}

.sketch-btn-text.good {
  fill: #66bb6a;
}

.sketch-btn-text.easy {
  fill: rgb(var(--v-theme-primary));
}

/* ========== 响应式 ========== */
@media (max-height: 700px) {
  .intro-screen {
    padding: 16px;
  }

  .main-title {
    font-size: 1.5rem;
  }

  .section-title {
    font-size: 1.2rem;
    margin-bottom: 16px;
  }

  .sub-text {
    margin-bottom: 24px;
  }

  .scroll-indicator {
    display: none;
  }
}

@media (max-width: 1400px) {
  .main-title {
    font-size: 1.5rem;
  }

  .section-title {
    font-size: 1.2rem;
  }
}

/* ========== 第六屏：欢迎加入 ========== */
.screen-inner-welcome {
  max-width: 540px;
}

.front-image {
  width: 500px;
  height: auto;
  margin-bottom: 32px;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.1));
}

.welcome-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: rgb(var(--v-theme-on-surface));
  margin: 0 0 16px 0;
}

.welcome-subtitle {
  font-size: 1rem;
  line-height: 1.8;
  color: rgba(var(--v-theme-on-surface), 0.6);
  margin: 0 0 32px 0;
}

.welcome-cta {
  font-size: 1.1rem;
  font-weight: 600;
  color: rgb(var(--v-theme-primary));
  margin: 0;
  padding: 12px 24px;
  background: rgba(var(--v-theme-primary), 0.1);
  border-radius: 24px;
  display: inline-block;
}
</style>
