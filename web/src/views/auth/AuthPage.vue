<template>
  <div class="auth-page">
    <!-- 背景装饰：学习热力图 —— 知识点打卡矩阵，呼应"可量化、高效、持续"的产品核心 -->
    <div class="bg-decor" aria-hidden="true">
      <div class="heatmap-wrap">
        <svg class="heatmap" viewBox="0 0 1600 900" preserveAspectRatio="xMidYMid slice">
          <defs>
            <!-- 5 级热度色阶：从空到主色满 -->
            <pattern id="cell-grid" x="0" y="0" width="24" height="24" patternUnits="userSpaceOnUse">
              <rect x="1" y="1" width="22" height="22" rx="4" fill="rgb(var(--v-theme-on-surface))" fill-opacity="0.035" />
            </pattern>
          </defs>

          <!-- 底层：均匀的空格子矩阵 -->
          <rect x="0" y="0" width="1600" height="900" fill="url(#cell-grid)" />

          <!-- 高亮层：不同热度的"已学习"格子，分布模拟真实打卡记录 -->
          <g class="heat-cells">
            <!-- 课程知识地图 -->
            <rect x="97" y="97" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="121" y="1" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="121" y="25" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="121" y="73" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="121" y="97" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="145" y="1" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="145" y="25" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="145" y="49" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="145" y="73" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="145" y="97" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="145" y="121" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="169" y="1" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="169" y="25" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="169" y="49" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="169" y="73" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="169" y="97" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="193" y="1" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="193" y="25" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="193" y="49" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="193" y="73" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="193" y="97" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="193" y="121" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="217" y="1" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="217" y="25" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="217" y="49" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="217" y="73" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="217" y="97" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="241" y="1" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="241" y="25" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="241" y="49" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="241" y="73" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="241" y="97" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="577" y="1" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="601" y="1" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="601" y="25" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="625" y="1" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="625" y="25" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="625" y="49" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="649" y="1" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="649" y="25" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="649" y="49" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="649" y="73" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="649" y="97" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="673" y="1" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="673" y="25" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="673" y="49" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="673" y="73" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="673" y="97" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="673" y="121" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="697" y="1" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="697" y="25" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="697" y="49" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="697" y="73" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="697" y="97" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="697" y="121" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="721" y="25" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="721" y="49" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="721" y="73" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="745" y="49" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="769" y="49" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1201" y="73" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1225" y="25" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1225" y="49" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1225" y="73" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1225" y="97" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1225" y="121" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1249" y="25" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1249" y="49" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1249" y="73" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1249" y="97" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1249" y="121" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1249" y="145" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1249" y="169" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1273" y="49" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1273" y="73" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1273" y="97" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1273" y="121" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1273" y="145" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1273" y="169" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1297" y="73" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1297" y="97" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1297" y="121" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1297" y="145" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1297" y="169" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1321" y="73" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1321" y="97" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1321" y="121" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1321" y="145" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1321" y="169" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1345" y="145" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="49" y="265" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="49" y="313" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="73" y="217" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="73" y="241" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="73" y="265" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="73" y="289" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="73" y="313" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="73" y="337" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="97" y="217" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="97" y="241" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="97" y="265" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="97" y="289" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="97" y="313" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="121" y="193" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="121" y="217" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="121" y="241" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="121" y="265" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="121" y="289" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="121" y="313" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="145" y="193" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="145" y="217" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="145" y="241" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="145" y="265" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="145" y="289" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="145" y="313" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="169" y="241" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="169" y="265" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="169" y="289" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="169" y="313" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="169" y="337" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="193" y="241" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="193" y="265" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="193" y="289" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="193" y="313" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="745" y="289" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="769" y="289" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="769" y="313" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="769" y="337" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="793" y="289" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="793" y="313" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="793" y="337" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="793" y="361" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="817" y="217" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="817" y="241" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="817" y="265" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="817" y="289" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="817" y="313" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="817" y="337" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="841" y="217" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="841" y="241" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="841" y="265" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="841" y="289" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="841" y="313" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="841" y="337" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="865" y="217" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="865" y="241" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="865" y="265" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="865" y="289" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="865" y="313" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="865" y="337" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="889" y="265" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="889" y="289" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="889" y="313" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="913" y="265" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="1297" y="217" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1321" y="193" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1321" y="217" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1321" y="241" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1321" y="265" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1345" y="193" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1345" y="217" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1345" y="241" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1345" y="265" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1345" y="289" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1369" y="193" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1369" y="217" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1369" y="241" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1369" y="265" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1393" y="193" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1393" y="217" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1393" y="241" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1393" y="265" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1393" y="289" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1417" y="193" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1417" y="217" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1417" y="241" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1417" y="265" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1417" y="289" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1441" y="193" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1441" y="217" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1441" y="241" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1441" y="265" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1465" y="193" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1465" y="217" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1465" y="241" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1465" y="265" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="193" y="385" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="193" y="409" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="217" y="409" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="217" y="433" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="241" y="409" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="241" y="433" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="241" y="457" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="265" y="385" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="265" y="409" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="265" y="433" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="265" y="457" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="289" y="385" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="289" y="409" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="289" y="433" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="289" y="457" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="289" y="481" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="313" y="385" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="313" y="409" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="313" y="433" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="313" y="457" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="337" y="409" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="337" y="433" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="337" y="457" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="337" y="481" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="361" y="409" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="361" y="433" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="361" y="457" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="361" y="481" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="385" y="433" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="385" y="457" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="793" y="385" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="817" y="385" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="817" y="409" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="817" y="457" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="841" y="385" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="841" y="409" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="841" y="433" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="841" y="457" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="841" y="481" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="841" y="505" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="865" y="385" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="865" y="409" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="865" y="433" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="865" y="457" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="865" y="481" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="865" y="505" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="889" y="433" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="889" y="457" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="889" y="481" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="889" y="505" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="913" y="409" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="913" y="433" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="913" y="457" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="913" y="481" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="913" y="505" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="937" y="433" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="937" y="457" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="937" y="481" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="937" y="505" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="961" y="457" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="961" y="481" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="961" y="505" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1369" y="505" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1369" y="529" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1393" y="433" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1393" y="481" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1393" y="505" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1393" y="529" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1417" y="433" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1417" y="457" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1417" y="481" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1417" y="505" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1417" y="529" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1441" y="385" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1441" y="409" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1441" y="433" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1441" y="457" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1441" y="481" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1441" y="505" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1441" y="529" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1465" y="433" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1465" y="457" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1465" y="481" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1465" y="505" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1465" y="529" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1489" y="433" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1489" y="481" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1489" y="505" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1489" y="529" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="1513" y="481" width="22" height="22" rx="4" fill="#f59e0b" fill-opacity="0.14" />
            <rect x="385" y="577" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="385" y="601" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="409" y="553" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="409" y="577" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="409" y="601" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="409" y="625" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="433" y="553" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="433" y="577" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="433" y="601" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="433" y="649" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="433" y="697" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="433" y="721" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="457" y="553" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="457" y="577" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="457" y="601" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="457" y="625" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="457" y="649" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="457" y="673" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="457" y="697" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="457" y="721" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="481" y="577" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="481" y="601" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="481" y="625" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="481" y="649" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="481" y="673" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="481" y="697" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="505" y="577" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="505" y="601" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="505" y="625" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="505" y="649" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="529" y="577" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="529" y="601" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="553" y="577" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="553" y="601" width="22" height="22" rx="4" fill="#06b6d4" fill-opacity="0.14" />
            <rect x="1057" y="649" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1057" y="673" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1081" y="625" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1081" y="649" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1081" y="673" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1081" y="697" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1105" y="553" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1105" y="577" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1105" y="601" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1105" y="625" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1105" y="649" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1105" y="673" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1105" y="697" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1105" y="721" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1129" y="601" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1129" y="625" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1129" y="649" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1129" y="673" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1129" y="697" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1129" y="721" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1153" y="601" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1153" y="625" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1153" y="649" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1153" y="673" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1153" y="697" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1153" y="721" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1177" y="625" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1177" y="649" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1177" y="673" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1177" y="697" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1177" y="721" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="1201" y="697" width="22" height="22" rx="4" fill="#ec4899" fill-opacity="0.14" />
            <rect x="97" y="817" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="121" y="745" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="121" y="769" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="121" y="793" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="121" y="817" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="121" y="841" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="145" y="745" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="145" y="769" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="145" y="793" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="145" y="817" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="145" y="841" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="145" y="865" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="169" y="721" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="169" y="745" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="169" y="769" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="169" y="793" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="169" y="817" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="169" y="841" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="169" y="865" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="169" y="889" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="193" y="721" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="193" y="745" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="193" y="769" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="193" y="793" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="193" y="817" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="193" y="841" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="217" y="721" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="217" y="745" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="217" y="769" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="217" y="793" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="217" y="817" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="217" y="841" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="241" y="793" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="241" y="817" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="241" y="841" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="241" y="865" width="22" height="22" rx="4" fill="#8b5cf6" fill-opacity="0.14" />
            <rect x="649" y="817" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="649" y="841" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="673" y="793" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="673" y="817" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="673" y="841" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="697" y="793" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="697" y="817" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="697" y="841" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="697" y="865" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="697" y="889" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="721" y="769" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="721" y="793" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="721" y="817" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="721" y="841" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="721" y="865" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="745" y="769" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="745" y="793" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="745" y="817" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="745" y="841" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="745" y="889" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="769" y="769" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="769" y="793" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="769" y="817" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="769" y="841" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="769" y="865" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="769" y="889" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="793" y="745" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="793" y="769" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="793" y="793" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="793" y="817" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="793" y="841" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="793" y="865" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="793" y="889" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="817" y="793" width="22" height="22" rx="4" fill="#10b981" fill-opacity="0.14" />
            <rect x="1297" y="721" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1297" y="745" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1297" y="769" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1297" y="793" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1297" y="817" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1321" y="721" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1321" y="745" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1321" y="769" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1321" y="793" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1321" y="817" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1321" y="841" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1321" y="865" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1345" y="721" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1345" y="745" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1345" y="769" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1345" y="793" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1345" y="817" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1345" y="841" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1369" y="721" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1369" y="745" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1369" y="769" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1369" y="793" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1369" y="817" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1369" y="841" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1393" y="745" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1393" y="769" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1393" y="793" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1393" y="817" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1393" y="841" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1417" y="769" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1417" y="793" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1417" y="817" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1417" y="841" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1417" y="865" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1441" y="793" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
            <rect x="1441" y="817" width="22" height="22" rx="4" fill="#3b82f6" fill-opacity="0.14" />
          </g>
        </svg>
      </div>
    </div>

    <SimpleHeader />

    <v-container fluid class="auth-container">
      <v-row align="center" justify="center">
        <!-- Left side - Website Introduction -->
        <v-col cols="12" lg="6" class="intro-section d-none d-lg-flex">
          <IntroSection />
        </v-col>

        <!-- Right side - Auth Form -->
        <v-col cols="12" sm="12" md="8" lg="4" class="d-flex justify-center">
          <div class="auth-card-wrapper">
            <v-card class="auth-card" rounded="xl" elevation="0">
              <!-- Logo Section -->
              <v-card-text class="text-center pt-10 pb-2 px-8">
                <div
                  class="logo-with-slogan"
                  @mouseenter="showSlogan = true"
                  @mouseleave="showSlogan = false"
                >
                  <IcosahedronLogo :size="72" />
                  <span class="logo-slogan" :class="{ visible: showSlogan }">{{
                    t('intro.logoSlogan')
                  }}</span>
                </div>

                <!-- 动态标题 -->
                <Transition name="fade" mode="out-in">
                  <div :key="currentMode">
                    <h2 class="text-h4 font-weight-bold mb-2">
                      {{ modeConfig.title }}
                    </h2>
                    <p class="text-body-2 text-medium-emphasis">{{ modeConfig.subtitle }}</p>
                  </div>
                </Transition>
              </v-card-text>

              <!-- 动态表单区域 -->
              <v-card-text class="px-8 pb-8">
                <Transition name="slide-fade" mode="out-in">
                  <!-- 登录表单 -->
                  <v-form
                    v-if="currentMode === 'login'"
                    key="login"
                    ref="loginFormRef"
                    v-model="loginFormValid"
                    @submit.prevent="handleLogin"
                  >
                    <v-text-field
                      v-model="loginData.email"
                      :label="t('user.login.email')"
                      :placeholder="t('user.login.emailPlaceholder')"
                      :rules="emailRules"
                      :counter="emailMaxLength"
                      :maxlength="emailMaxLength"
                      :disabled="isLoggingIn"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    />

                    <v-text-field
                      v-model="loginData.password"
                      :label="t('user.login.password')"
                      :placeholder="t('user.login.passwordPlaceholder')"
                      :type="showLoginPassword ? 'text' : 'password'"
                      :maxlength="passwordMaxLength"
                      :disabled="isLoggingIn"
                      variant="outlined"
                      density="comfortable"
                      class="mb-2"
                    >
                      <template #append-inner>
                        <v-icon
                          tabindex="-1"
                          style="cursor: pointer"
                          @click="showLoginPassword = !showLoginPassword"
                        >
                          {{ showLoginPassword ? 'mdi-eye-off' : 'mdi-eye' }}
                        </v-icon>
                      </template>
                    </v-text-field>

                    <div class="text-right mb-6">
                      <a
                        href="#"
                        class="text-caption text-primary text-decoration-none"
                        @click.prevent="switchToForgot"
                      >
                        {{ t('user.login.forgotPassword') }}
                      </a>
                    </div>

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <TurnstileWidget
                      v-if="showLoginCaptcha"
                      ref="loginTurnstileRef"
                      class="mb-4"
                      @verify="onLoginTurnstileVerify"
                      @error="onTurnstileError"
                      @expire="onLoginTurnstileExpire"
                    />

                    <v-btn
                      type="submit"
                      :loading="isLoggingIn"
                      :disabled="
                        isLoggingIn ||
                        !loginFormValid ||
                        !loginData.email ||
                        !loginData.password
                      "
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-3"
                    >
                      {{ isLoggingIn ? t('user.login.loggingIn') : t('user.login.loginButton') }}
                    </v-btn>

                    <p class="text-caption text-center text-medium-emphasis mb-4">
                      {{ t('user.login.agreementPrefix') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.userAgreement')
                      }}</a>
                      {{ t('user.login.agreementMiddle') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.privacyPolicy')
                      }}</a
                      >。
                    </p>

                    <v-divider class="my-6" />

                    <div class="text-center">
                      <span class="text-body-2 text-medium-emphasis">
                        {{ t('user.login.registerLink') }}
                      </span>
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                        @click.prevent="switchToRegister"
                      >
                        {{ t('user.login.registerNow') }}
                      </a>
                    </div>
                  </v-form>

                  <!-- 注册表单 -->
                  <v-form
                    v-else-if="currentMode === 'register'"
                    key="register"
                    ref="registerFormRef"
                    v-model="registerFormValid"
                    @submit.prevent="handleRegister"
                  >
                    <v-text-field
                      v-model="registerData.email"
                      :label="t('user.register.email')"
                      type="email"
                      :rules="emailRules"
                      :counter="emailMaxLength"
                      :maxlength="emailMaxLength"
                      :disabled="isRegistering"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    />

                    <v-text-field
                      v-model="registerData.password"
                      :label="t('user.register.password')"
                      :type="showRegisterPassword ? 'text' : 'password'"
                      :rules="passwordRules"
                      :counter="passwordMaxLength"
                      :maxlength="passwordMaxLength"
                      :disabled="isRegistering"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    >
                      <template #append-inner>
                        <div
                          v-if="registerData.password"
                          class="strength-segments mr-2"
                        >
                          <span
                            class="text-caption strength-label"
                            :class="`text-${passwordStrengthColor}`"
                          >
                            {{ passwordStrengthLabel }}
                          </span>
                          <span
                            v-for="i in 5"
                            :key="i"
                            class="seg"
                            :class="i <= passwordScore + 1 ? `bg-${passwordStrengthColor}` : ''"
                          />
                        </div>
                        <v-icon
                          tabindex="-1"
                          style="cursor: pointer"
                          @click="showRegisterPassword = !showRegisterPassword"
                        >
                          {{ showRegisterPassword ? 'mdi-eye-off' : 'mdi-eye' }}
                        </v-icon>
                      </template>
                    </v-text-field>

                    <!-- 密码强度条已移入输入框内 -->

                    <v-text-field
                      v-model="registerData.confirmPassword"
                      :label="t('user.register.confirmPassword')"
                      :type="showConfirmPassword ? 'text' : 'password'"
                      :rules="confirmPasswordRules"
                      :counter="passwordMaxLength"
                      :maxlength="passwordMaxLength"
                      :disabled="isRegistering"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-2"
                    >
                      <template #append-inner>
                        <v-icon
                          tabindex="-1"
                          style="cursor: pointer"
                          @click="showConfirmPassword = !showConfirmPassword"
                        >
                          {{ showConfirmPassword ? 'mdi-eye-off' : 'mdi-eye' }}
                        </v-icon>
                      </template>
                    </v-text-field>

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <TurnstileWidget
                      ref="registerTurnstileRef"
                      appearance="interaction-only"
                      class="mb-4"
                      @verify="onRegisterTurnstileVerify"
                      @error="onTurnstileError"
                      @expire="onRegisterTurnstileExpire"
                    />

                    <v-btn
                      type="submit"
                      :loading="isRegistering"
                      :disabled="
                        isRegistering ||
                        !registerFormValid ||
                        !passwordAcceptable ||
                        !registerData.email ||
                        !registerData.password ||
                        !registerData.confirmPassword
                      "
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-3"
                    >
                      {{
                        isRegistering
                          ? t('user.register.registering')
                          : t('user.register.registerButton')
                      }}
                    </v-btn>

                    <p class="text-caption text-center text-medium-emphasis mb-4">
                      {{ t('user.login.agreementPrefix') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.userAgreement')
                      }}</a>
                      {{ t('user.login.agreementMiddle') }}
                      <a href="#" class="text-primary text-decoration-none">{{
                        t('user.login.privacyPolicy')
                      }}</a
                      >。
                    </p>

                    <v-divider class="my-6" />

                    <div class="text-center">
                      <span class="text-body-2 text-medium-emphasis">
                        {{ t('user.register.loginLink') }}
                      </span>
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                        @click.prevent="switchToLogin"
                      >
                        {{ t('user.register.loginNow') }}
                      </a>
                    </div>
                  </v-form>

                  <!-- 邮箱验证表单 -->
                  <v-form
                    v-else-if="currentMode === 'verify'"
                    key="verify"
                    ref="verifyFormRef"
                    @submit.prevent="handleVerify"
                  >
                    <v-alert
                      color="primary"
                      variant="tonal"
                      density="compact"
                      rounded="lg"
                      class="mb-4"
                    >
                      <div class="d-flex align-center">
                        <v-icon size="20" class="mr-2">mdi-email-outline</v-icon>
                        <span class="text-body-2">
                          {{ t('user.verifyEmail.codeSentTo') }}
                          <strong>{{ verifyEmail }}</strong>
                        </span>
                      </div>
                    </v-alert>

                    <v-text-field
                      v-model="verifyData.code"
                      :label="t('user.verifyEmail.codeLabel')"
                      :placeholder="t('user.verifyEmail.codePlaceholder')"
                      :rules="[verificationCodeRule]"
                      :disabled="isVerifying"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                      counter="6"
                      maxlength="6"
                      @input="verifyData.code = verifyData.code.replace(/\D/g, '')"
                    />

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <v-btn
                      type="submit"
                      :loading="isVerifying"
                      :disabled="isVerifying || verifyData.code.length !== 6"
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-3"
                    >
                      {{
                        isVerifying
                          ? t('user.verifyEmail.verifying')
                          : t('user.verifyEmail.verifyButton')
                      }}
                    </v-btn>

                    <div class="text-center mb-4">
                      <v-btn
                        variant="text"
                        color="primary"
                        size="small"
                        :disabled="isResending || countdown > 0"
                        :loading="isResending"
                        @click="handleResend"
                      >
                        {{
                          countdown > 0
                            ? t('user.verifyEmail.resendCountdown', { seconds: countdown })
                            : t('user.verifyEmail.resendButton')
                        }}
                      </v-btn>
                    </div>

                    <v-divider class="my-6" />

                    <div class="text-center">
                      <span class="text-body-2 text-medium-emphasis">
                        {{ t('user.verifyEmail.alreadyVerified') }}
                      </span>
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold ml-1"
                        @click.prevent="switchToLogin"
                      >
                        {{ t('user.verifyEmail.loginNow') }}
                      </a>
                    </div>
                  </v-form>

                  <!-- 忘记密码：输入邮箱 -->
                  <v-form
                    v-else-if="currentMode === 'forgot'"
                    key="forgot"
                    ref="forgotFormRef"
                    v-model="forgotFormValid"
                    @submit.prevent="handleRequestReset"
                  >
                    <v-text-field
                      v-model="forgotData.email"
                      :label="t('user.forgotPassword.emailLabel')"
                      :placeholder="t('user.forgotPassword.emailPlaceholder')"
                      :rules="emailRules"
                      :counter="emailMaxLength"
                      :maxlength="emailMaxLength"
                      :disabled="isRequestingReset"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    />

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <TurnstileWidget
                      ref="forgotTurnstileRef"
                      appearance="interaction-only"
                      class="mb-4"
                      @verify="onForgotTurnstileVerify"
                      @error="onTurnstileError"
                      @expire="onForgotTurnstileExpire"
                    />

                    <v-btn
                      type="submit"
                      :loading="isRequestingReset"
                      :disabled="
                        isRequestingReset ||
                        !forgotFormValid ||
                        !forgotData.email ||
                        !forgotTurnstileToken
                      "
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-3"
                    >
                      {{
                        isRequestingReset
                          ? t('user.forgotPassword.submitting')
                          : t('user.forgotPassword.submitButton')
                      }}
                    </v-btn>

                    <v-divider class="my-6" />

                    <div class="text-center">
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold"
                        @click.prevent="switchToLogin"
                      >
                        {{ t('user.forgotPassword.backToLogin') }}
                      </a>
                    </div>
                  </v-form>

                  <!-- 忘记密码：输入验证码 -->
                  <v-form
                    v-else-if="currentMode === 'reset-verify'"
                    key="reset-verify"
                    ref="resetVerifyFormRef"
                    @submit.prevent="handleVerifyResetCode"
                  >
                    <v-alert
                      color="primary"
                      variant="tonal"
                      density="compact"
                      rounded="lg"
                      class="mb-4"
                    >
                      <div class="d-flex align-center">
                        <v-icon size="20" class="mr-2">mdi-email-outline</v-icon>
                        <span class="text-body-2">
                          {{ t('user.verifyEmail.codeSentTo') }}
                          <strong>{{ resetEmail }}</strong>
                        </span>
                      </div>
                    </v-alert>

                    <v-text-field
                      v-model="resetVerifyData.code"
                      :label="t('user.verifyEmail.codeLabel')"
                      :placeholder="t('user.verifyEmail.codePlaceholder')"
                      :rules="[verificationCodeRule]"
                      :disabled="isVerifyingReset"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                      counter="6"
                      maxlength="6"
                      @input="resetVerifyData.code = resetVerifyData.code.replace(/\D/g, '')"
                    />

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <v-btn
                      type="submit"
                      :loading="isVerifyingReset"
                      :disabled="isVerifyingReset || resetVerifyData.code.length !== 6"
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-3"
                    >
                      {{
                        isVerifyingReset
                          ? t('user.verifyEmail.verifying')
                          : t('user.verifyEmail.verifyButton')
                      }}
                    </v-btn>

                    <div class="text-center mb-4">
                      <v-btn
                        variant="text"
                        color="primary"
                        size="small"
                        :disabled="isResendingReset || countdown > 0"
                        :loading="isResendingReset"
                        @click="handleResendResetCode"
                      >
                        {{
                          countdown > 0
                            ? t('user.verifyEmail.resendCountdown', { seconds: countdown })
                            : t('user.verifyEmail.resendButton')
                        }}
                      </v-btn>
                    </div>

                    <v-divider class="my-6" />

                    <div class="text-center">
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold"
                        @click.prevent="switchToLogin"
                      >
                        {{ t('user.forgotPassword.backToLogin') }}
                      </a>
                    </div>
                  </v-form>

                  <!-- 忘记密码：设置新密码 -->
                  <v-form
                    v-else-if="currentMode === 'reset-confirm'"
                    key="reset-confirm"
                    ref="resetConfirmFormRef"
                    v-model="resetConfirmFormValid"
                    @submit.prevent="handleConfirmReset"
                  >
                    <v-text-field
                      v-model="resetConfirmData.password"
                      :label="t('user.forgotPassword.newPassword')"
                      :type="showResetPassword ? 'text' : 'password'"
                      :rules="passwordRules"
                      :counter="passwordMaxLength"
                      :maxlength="passwordMaxLength"
                      :disabled="isConfirmingReset"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-4"
                    >
                      <template #append-inner>
                        <div
                          v-if="resetConfirmData.password"
                          class="strength-segments mr-2"
                        >
                          <span
                            class="text-caption strength-label"
                            :class="`text-${resetPasswordStrengthColor}`"
                          >
                            {{ resetPasswordStrengthLabel }}
                          </span>
                          <span
                            v-for="i in 5"
                            :key="i"
                            class="seg"
                            :class="
                              i <= resetPasswordScore + 1
                                ? `bg-${resetPasswordStrengthColor}`
                                : ''
                            "
                          />
                        </div>
                        <v-icon
                          tabindex="-1"
                          style="cursor: pointer"
                          @click="showResetPassword = !showResetPassword"
                        >
                          {{ showResetPassword ? 'mdi-eye-off' : 'mdi-eye' }}
                        </v-icon>
                      </template>
                    </v-text-field>

                    <v-text-field
                      v-model="resetConfirmData.confirmPassword"
                      :label="t('user.forgotPassword.confirmPassword')"
                      :type="showResetConfirmPassword ? 'text' : 'password'"
                      :rules="resetConfirmPasswordRules"
                      :counter="passwordMaxLength"
                      :maxlength="passwordMaxLength"
                      :disabled="isConfirmingReset"
                      validate-on="blur"
                      variant="outlined"
                      density="comfortable"
                      class="mb-2"
                    >
                      <template #append-inner>
                        <v-icon
                          tabindex="-1"
                          style="cursor: pointer"
                          @click="showResetConfirmPassword = !showResetConfirmPassword"
                        >
                          {{ showResetConfirmPassword ? 'mdi-eye-off' : 'mdi-eye' }}
                        </v-icon>
                      </template>
                    </v-text-field>

                    <div v-if="errorMessage" class="error-message mb-4">
                      <v-icon size="16" class="mr-1">mdi-alert-circle</v-icon>
                      {{ errorMessage }}
                    </div>

                    <v-btn
                      type="submit"
                      :loading="isConfirmingReset"
                      :disabled="
                        isConfirmingReset ||
                        !resetConfirmFormValid ||
                        !resetPasswordAcceptable ||
                        !resetConfirmData.password ||
                        !resetConfirmData.confirmPassword
                      "
                      block
                      size="large"
                      color="primary"
                      class="text-none font-weight-bold mb-3"
                    >
                      {{
                        isConfirmingReset
                          ? t('user.forgotPassword.submittingReset')
                          : t('user.forgotPassword.submitReset')
                      }}
                    </v-btn>

                    <v-divider class="my-6" />

                    <div class="text-center">
                      <a
                        href="#"
                        class="text-body-2 text-primary text-decoration-none font-weight-bold"
                        @click.prevent="switchToLogin"
                      >
                        {{ t('user.forgotPassword.backToLogin') }}
                      </a>
                    </div>
                  </v-form>
                </Transition>
              </v-card-text>
            </v-card>
          </div>
        </v-col>
      </v-row>
    </v-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useAuth } from '@/composables/useAuth'
import { usePasswordStrength } from '@/composables/usePasswordStrength'
import {
  useEmailRules,
  useValidationRules,
  useMaxLength,
  useConfirmPasswordRule,
  useVerificationCodeRule,
} from '@/composables/useValidation'
import { HEADER_HEIGHT } from '@/constants/layout'
import { BUSINESS_ERROR } from '@/constants/errorCode'
import { useAuthStore } from '@/stores'
import { useUserStore } from '@/stores'
import SimpleHeader from '@/components/layout/SimpleHeader.vue'
import IntroSection from '@/components/common/IntroSection.vue'
import TurnstileWidget from '@/components/common/TurnstileWidget.vue'
import IcosahedronLogo from '@/components/common/IcosahedronLogo.vue'

type AuthMode = 'login' | 'register' | 'verify' | 'forgot' | 'reset-verify' | 'reset-confirm'

const router = useRouter()
const route = useRoute()
const { t } = useI18n()
const { login, register, isLoggingIn, isRegistering } = useAuth()
const authStore = useAuthStore()
const userStore = useUserStore()
const showSnackbar = inject<(message: string, type?: string) => void>('showSnackbar')

// 当前模式
const currentMode = ref<AuthMode>('login')

// 验证规则
const emailRules = useEmailRules()
const passwordRules = useValidationRules('password')
const confirmPasswordRule = useConfirmPasswordRule()
const verificationCodeRule = useVerificationCodeRule()
const emailMaxLength = useMaxLength('email')
const passwordMaxLength = useMaxLength('password')

// 表单引用
const loginFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const registerFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const verifyFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)

// 表单校验状态（由 v-form v-model 同步）
const loginFormValid = ref(false)
const registerFormValid = ref(false)

// Logo slogan
const showSlogan = ref(false)

// 登录数据
const loginData = ref({
  email: '',
  password: '',
})

// 注册数据
const registerData = ref({
  email: '',
  password: '',
  confirmPassword: '',
})
const registerPassword = computed(() => registerData.value.password)
const {
  score: passwordScore,
  label: passwordStrengthLabel,
  color: passwordStrengthColor,
  isAcceptable: passwordAcceptable,
} = usePasswordStrength(registerPassword)

// 验证数据
const verifyData = ref({
  code: '',
})
const verifyEmail = computed(() => authStore.pendingSession?.email ?? '')

// 忘记密码 —— 步骤 1：输入邮箱
const forgotData = ref({ email: '' })
const forgotFormValid = ref(false)
const forgotFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const isRequestingReset = ref(false)
const forgotTurnstileToken = ref('')
const forgotTurnstileRef = ref<InstanceType<typeof TurnstileWidget> | null>(null)

// 忘记密码 —— 步骤 2：输入验证码
const resetVerifyData = ref({ code: '' })
const resetVerifyFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const isVerifyingReset = ref(false)
const isResendingReset = ref(false)
const resetEmail = computed(() => authStore.resetSession?.email ?? '')

// 忘记密码 —— 步骤 3：设置新密码
const resetConfirmData = ref({ password: '', confirmPassword: '' })
const resetConfirmFormValid = ref(false)
const resetConfirmFormRef = ref<{ validate: () => Promise<{ valid: boolean }> } | null>(null)
const isConfirmingReset = ref(false)
const showResetPassword = ref(false)
const showResetConfirmPassword = ref(false)

const resetPasswordInput = computed(() => resetConfirmData.value.password)
const {
  score: resetPasswordScore,
  label: resetPasswordStrengthLabel,
  color: resetPasswordStrengthColor,
  isAcceptable: resetPasswordAcceptable,
} = usePasswordStrength(resetPasswordInput)

const resetConfirmPasswordRules = computed(() => [
  ...passwordRules.value,
  confirmPasswordRule(resetConfirmData.value.password),
])

// UI 状态
const showLoginPassword = ref(false)
const showRegisterPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref('')
let errorTimer: ReturnType<typeof setTimeout> | null = null

// Turnstile 状态
const showLoginCaptcha = ref(false)
const loginTurnstileToken = ref('')
const registerTurnstileToken = ref('')
const loginTurnstileRef = ref<InstanceType<typeof TurnstileWidget> | null>(null)
const registerTurnstileRef = ref<InstanceType<typeof TurnstileWidget> | null>(null)

// 验证页面状态
const isVerifying = ref(false)
const isResending = ref(false)
const countdown = ref(0)
let countdownTimer: number | null = null

// 模式配置
const modeConfig = computed(() => {
  switch (currentMode.value) {
    case 'login':
      return {
        title: t('user.login.title'),
        subtitle: t('user.login.subtitle'),
      }
    case 'register':
      return {
        title: t('user.register.title'),
        subtitle: t('user.register.subtitle'),
      }
    case 'verify':
      return {
        title: t('user.verifyEmail.title'),
        subtitle: t('user.verifyEmail.subtitle'),
      }
    case 'forgot':
      return {
        title: t('user.forgotPassword.title'),
        subtitle: t('user.forgotPassword.subtitle'),
      }
    case 'reset-verify':
      return {
        title: t('user.verifyEmail.title'),
        subtitle: t('user.verifyEmail.subtitle'),
      }
    case 'reset-confirm':
      return {
        title: t('user.forgotPassword.resetTitle'),
        subtitle: t('user.forgotPassword.resetSubtitle'),
      }
    default:
      return {
        title: '',
        subtitle: '',
      }
  }
})

// 确认密码验证规则
const confirmPasswordRules = computed(() => [
  ...passwordRules.value,
  confirmPasswordRule(registerData.value.password),
])

// 显示错误消息，5秒后自动消失
const showError = (message: string) => {
  if (errorTimer) {
    clearTimeout(errorTimer)
  }
  errorMessage.value = message
  errorTimer = setTimeout(() => {
    errorMessage.value = ''
  }, 5000)
}

// 清除错误
const clearError = () => {
  errorMessage.value = ''
  if (errorTimer) {
    clearTimeout(errorTimer)
  }
}

// Turnstile 回调
const onLoginTurnstileVerify = (token: string) => {
  loginTurnstileToken.value = token
}

const onRegisterTurnstileVerify = (token: string) => {
  registerTurnstileToken.value = token
}

const onTurnstileError = () => {
  showError(t('user.login.captchaLoadFailed'))
}

const onLoginTurnstileExpire = () => {
  loginTurnstileToken.value = ''
}

const onRegisterTurnstileExpire = () => {
  registerTurnstileToken.value = ''
}

// 切换模式
const switchToLogin = () => {
  clearError()
  currentMode.value = 'login'
  void router.replace('/login')
}

const switchToRegister = () => {
  clearError()
  currentMode.value = 'register'
  void router.replace('/register')
}

const switchToVerify = () => {
  clearError()
  verifyData.value.code = ''
  currentMode.value = 'verify'
  // 基于后端返回的 resendAvailableIn 起倒计时
  const pending = authStore.pendingSession
  if (pending && pending.resendAvailableIn > 0) {
    startCountdown(pending.resendAvailableIn)
  } else {
    stopCountdown()
  }
  void router.replace('/verify-email')
}

const switchToForgot = () => {
  clearError()
  forgotData.value.email = ''
  forgotTurnstileToken.value = ''
  currentMode.value = 'forgot'
  void router.replace('/forgot-password')
}

const switchToResetVerify = () => {
  clearError()
  resetVerifyData.value.code = ''
  currentMode.value = 'reset-verify'
  const reset = authStore.resetSession
  if (reset && reset.resendAvailableIn > 0) {
    startCountdown(reset.resendAvailableIn)
  } else {
    stopCountdown()
  }
  void router.replace('/reset-password/verify')
}

const switchToResetConfirm = () => {
  clearError()
  resetConfirmData.value.password = ''
  resetConfirmData.value.confirmPassword = ''
  currentMode.value = 'reset-confirm'
  void router.replace('/reset-password/confirm')
}

// Turnstile 回调（忘记密码）
const onForgotTurnstileVerify = (token: string) => {
  forgotTurnstileToken.value = token
}
const onForgotTurnstileExpire = () => {
  forgotTurnstileToken.value = ''
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  const { valid } = await loginFormRef.value.validate()
  if (!valid) return

  if (showLoginCaptcha.value && !loginTurnstileToken.value) {
    showError(t('user.login.captchaRequired'))
    return
  }

  try {
    clearError()
    const token = showLoginCaptcha.value ? loginTurnstileToken.value : undefined
    const result = await login(loginData.value.email, loginData.value.password, token)
    if (result === 'pending') {
      switchToVerify()
    }
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }

    if (err.code === BUSINESS_ERROR.CAPTCHA_REQUIRED) {
      showLoginCaptcha.value = true
      showError(t('user.login.captchaTooManyFailed'))
      return
    }

    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.login.loginFailed'))
    }

    if (showLoginCaptcha.value) {
      loginTurnstileToken.value = ''
      loginTurnstileRef.value?.reset()
    }
  }
}

// 处理注册
const handleRegister = async () => {
  if (!registerFormRef.value) return
  const { valid } = await registerFormRef.value.validate()
  if (!valid) return

  if (!registerTurnstileToken.value) {
    showError(t('user.register.captchaRequired'))
    return
  }

  try {
    clearError()
    const success = await register(
      registerData.value.email,
      registerData.value.password,
      registerTurnstileToken.value
    )

    if (success) {
      switchToVerify()
    }
  } catch (error: unknown) {
    if (error instanceof Error && error.message) {
      showError(error.message)
    } else {
      showError(t('user.register.registerFailed'))
    }
    registerTurnstileToken.value = ''
    registerTurnstileRef.value?.reset()
  }
}

// 处理邮箱验证
const handleVerify = async () => {
  if (!verifyFormRef.value) return
  const { valid } = await verifyFormRef.value.validate()
  if (!valid) return

  if (!authStore.pendingSession) {
    showError(t('user.verifyEmail.emailInvalid'))
    switchToLogin()
    return
  }

  try {
    isVerifying.value = true
    clearError()

    const user = await authStore.validateEmail(verifyData.value.code)

    if (user) {
      userStore.setUser(user)
      showSnackbar?.(t('user.verifyEmail.verifySuccess'), 'success')
      await router.push('/')
    } else {
      showError(t('user.verifyEmail.verifyFailed'))
    }
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    // pending session 失效 → 回登录页
    if (err.code === BUSINESS_ERROR.PENDING_SESSION_INVALID) {
      authStore.clearPendingSession()
      showError(err.message ?? t('user.verifyEmail.emailInvalid'))
      switchToLogin()
      return
    }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.verifyEmail.verifyFailedRetry'))
    }
  } finally {
    isVerifying.value = false
  }
}

// 重新发送验证码
const handleResend = async () => {
  if (!authStore.pendingSession) {
    showError(t('user.verifyEmail.emailInvalid'))
    switchToLogin()
    return
  }

  try {
    isResending.value = true
    clearError()

    const next = await authStore.resendVerificationCode()
    showSnackbar?.(t('user.verifyEmail.codeSent'), 'success')
    if (next) {
      startCountdown(next.resendAvailableIn > 0 ? next.resendAvailableIn : 60)
    } else {
      startCountdown(60)
    }
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    if (err.code === BUSINESS_ERROR.PENDING_SESSION_INVALID) {
      authStore.clearPendingSession()
      showError(err.message ?? t('user.verifyEmail.emailInvalid'))
      switchToLogin()
      return
    }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.verifyEmail.sendFailed'))
    }
  } finally {
    isResending.value = false
  }
}

// ========== 忘记密码 handlers ==========

const handleRequestReset = async () => {
  if (!forgotFormRef.value) return
  const { valid } = await forgotFormRef.value.validate()
  if (!valid) return

  if (!forgotTurnstileToken.value) {
    showError(t('user.login.captchaRequired'))
    return
  }

  try {
    isRequestingReset.value = true
    clearError()
    await authStore.requestPasswordReset(forgotData.value.email, forgotTurnstileToken.value)
    switchToResetVerify()
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.forgotPassword.requestFailed'))
    }
    forgotTurnstileToken.value = ''
    forgotTurnstileRef.value?.reset()
  } finally {
    isRequestingReset.value = false
  }
}

const handleVerifyResetCode = async () => {
  if (!authStore.resetSession) {
    showError(t('user.forgotPassword.sessionExpired'))
    switchToForgot()
    return
  }

  try {
    isVerifyingReset.value = true
    clearError()
    const ok = await authStore.verifyPasswordResetCode(resetVerifyData.value.code)
    if (ok) {
      switchToResetConfirm()
    }
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    if (err.code === BUSINESS_ERROR.PASSWORD_RESET_SESSION_INVALID) {
      authStore.clearResetSession()
      showError(err.message ?? t('user.forgotPassword.sessionExpired'))
      switchToForgot()
      return
    }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.verifyEmail.verifyFailedRetry'))
    }
  } finally {
    isVerifyingReset.value = false
  }
}

const handleResendResetCode = async () => {
  if (!authStore.resetSession) {
    showError(t('user.forgotPassword.sessionExpired'))
    switchToForgot()
    return
  }

  try {
    isResendingReset.value = true
    clearError()
    const next = await authStore.resendPasswordResetCode()
    showSnackbar?.(t('user.verifyEmail.codeSent'), 'success')
    if (next) {
      startCountdown(next.resendAvailableIn > 0 ? next.resendAvailableIn : 60)
    } else {
      startCountdown(60)
    }
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    if (err.code === BUSINESS_ERROR.PASSWORD_RESET_SESSION_INVALID) {
      authStore.clearResetSession()
      showError(err.message ?? t('user.forgotPassword.sessionExpired'))
      switchToForgot()
      return
    }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.verifyEmail.sendFailed'))
    }
  } finally {
    isResendingReset.value = false
  }
}

const handleConfirmReset = async () => {
  if (!resetConfirmFormRef.value) return
  const { valid } = await resetConfirmFormRef.value.validate()
  if (!valid) return

  if (!authStore.resetSession) {
    showError(t('user.forgotPassword.sessionExpired'))
    switchToForgot()
    return
  }

  try {
    isConfirmingReset.value = true
    clearError()
    const ok = await authStore.confirmPasswordReset(resetConfirmData.value.password)
    if (ok) {
      showSnackbar?.(t('user.forgotPassword.resetSuccess'), 'success')
      switchToLogin()
    }
  } catch (error: unknown) {
    const err = error as { code?: number; message?: string }
    if (
      err.code === BUSINESS_ERROR.PASSWORD_RESET_SESSION_INVALID ||
      err.code === BUSINESS_ERROR.PASSWORD_RESET_NOT_VERIFIED
    ) {
      authStore.clearResetSession()
      showError(err.message ?? t('user.forgotPassword.sessionExpired'))
      switchToForgot()
      return
    }
    if (err.message) {
      showError(err.message)
    } else {
      showError(t('user.forgotPassword.resetFailed'))
    }
  } finally {
    isConfirmingReset.value = false
  }
}

// 倒计时
const startCountdown = (seconds = 60) => {
  countdown.value = seconds
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  countdownTimer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopCountdown()
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}

// 根据路由初始化模式
const initModeFromRoute = () => {
  const routeName = route.name as string
  if (routeName === 'register') {
    currentMode.value = 'register'
  } else if (routeName === 'verify-email') {
    // 必须有 pending session 才能进入验证页；否则回登录
    if (!authStore.pendingSession) {
      showSnackbar?.(t('user.verifyEmail.emailInvalidRedirect'), 'error')
      currentMode.value = 'login'
      void router.replace('/login')
      return
    }
    currentMode.value = 'verify'
    const pending = authStore.pendingSession
    if (pending.resendAvailableIn > 0) {
      startCountdown(pending.resendAvailableIn)
    }
  } else if (routeName === 'forgot-password') {
    currentMode.value = 'forgot'
  } else if (routeName === 'reset-password-verify') {
    if (!authStore.resetSession) {
      showSnackbar?.(t('user.forgotPassword.sessionExpired'), 'error')
      currentMode.value = 'forgot'
      void router.replace('/forgot-password')
      return
    }
    currentMode.value = 'reset-verify'
    const reset = authStore.resetSession
    if (reset.resendAvailableIn > 0) {
      startCountdown(reset.resendAvailableIn)
    }
  } else if (routeName === 'reset-password-confirm') {
    if (!authStore.resetSession) {
      showSnackbar?.(t('user.forgotPassword.sessionExpired'), 'error')
      currentMode.value = 'forgot'
      void router.replace('/forgot-password')
      return
    }
    currentMode.value = 'reset-confirm'
  } else {
    currentMode.value = 'login'
  }
}

// 监听路由变化
watch(
  () => route.name,
  () => {
    initModeFromRoute()
  }
)

onMounted(() => {
  initModeFromRoute()
})

onUnmounted(() => {
  stopCountdown()
  if (errorTimer) {
    clearTimeout(errorTimer)
  }
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background-color: rgb(var(--v-theme-background));
  position: relative;
  overflow: hidden;
}

/* ========== 背景装饰：学习热力图 ========== */
.bg-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.heatmap-wrap {
  position: absolute;
  inset: 0;
  opacity: 0.55;
  will-change: transform;
  /* 双层遮罩：左侧大范围镂空（引导区），右侧适中镂空（登录卡片），四角保留热力图 */
  mask-image:
    radial-gradient(
      ellipse 32% 90% at 28% 50%,
      transparent 0%,
      rgba(0, 0, 0, 0.3) 60%,
      black 100%
    ),
    radial-gradient(
      ellipse 22% 55% at 75% 50%,
      transparent 0%,
      transparent 40%,
      black 100%
    );
  mask-composite: intersect;
  -webkit-mask-image:
    radial-gradient(
      ellipse 32% 90% at 28% 50%,
      transparent 0%,
      rgba(0, 0, 0, 0.3) 60%,
      black 100%
    ),
    radial-gradient(
      ellipse 22% 55% at 75% 50%,
      transparent 0%,
      transparent 40%,
      black 100%
    );
  -webkit-mask-composite: source-in;
}

/* 统一降低热度色阶，避免与文字争视觉 */
.heat-1 { fill-opacity: 0.08 !important; }
.heat-2 { fill-opacity: 0.18 !important; }
.heat-3 { fill-opacity: 0.32 !important; }
.heat-4 { fill-opacity: 0.5 !important; }

.heatmap {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

/* 热度色阶：从空 → 浅 → 深，使用主题色 */
.heat-cells rect {
  transition: opacity 0.3s ease;
}

.heat-1 { fill: rgb(var(--v-theme-primary)); fill-opacity: 0.12; }
.heat-2 { fill: rgb(var(--v-theme-primary)); fill-opacity: 0.28; }
.heat-3 { fill: rgb(var(--v-theme-primary)); fill-opacity: 0.5; }
.heat-4 { fill: rgb(var(--v-theme-primary)); fill-opacity: 0.78; }

/* 正在打卡的焦点格子：呼吸动画，引导视觉 */
.heat-active {
  fill: rgb(var(--v-theme-primary));
  fill-opacity: 0.9;
  animation: cell-pulse 2.4s ease-in-out infinite;
  transform-origin: center;
  transform-box: fill-box;
}

.heat-active:nth-of-type(2) { animation-delay: -0.4s; }
.heat-active:nth-of-type(3) { animation-delay: -0.9s; }
.heat-active:nth-of-type(4) { animation-delay: -1.4s; }
.heat-active:nth-of-type(5) { animation-delay: -1.9s; }

@keyframes cell-pulse {
  0%, 100% {
    fill-opacity: 0.95;
    filter: drop-shadow(0 0 6px rgba(var(--v-theme-primary), 0.55));
  }
  50% {
    fill-opacity: 0.4;
    filter: drop-shadow(0 0 0 transparent);
  }
}

/* 角落极淡光晕，给背景一丝深度但不抢戏 */
.bg-decor::before {
  content: '';
  position: absolute;
  top: -15%;
  right: -10%;
  width: 700px;
  height: 700px;
  background: radial-gradient(
    circle,
    rgba(var(--v-theme-primary), 0.08) 0%,
    transparent 70%
  );
  pointer-events: none;
  border-radius: 50%;
}

.bg-decor::after {
  content: '';
  position: absolute;
  bottom: -20%;
  left: -12%;
  width: 800px;
  height: 800px;
  background: radial-gradient(
    circle,
    rgba(124, 58, 237, 0.07) 0%,
    transparent 70%
  );
  pointer-events: none;
  border-radius: 50%;
}


.auth-container {
  height: calc(100vh - v-bind('`${HEADER_HEIGHT}px`'));
  padding-top: 20px;
  padding-bottom: 20px;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  overflow-y: auto;
}

/* Introduction Section */
.intro-section {
  height: calc(100vh - v-bind('`${HEADER_HEIGHT}px`') - 40px);
  max-height: calc(100vh - v-bind('`${HEADER_HEIGHT}px`') - 40px);
  overflow: hidden;
  padding: 0;
}

/* Auth Card */
.auth-card-wrapper {
  width: 100%;
  max-width: 480px;
}

.auth-card {
  background-color: rgba(var(--v-theme-surface), 0.5) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(var(--v-theme-on-surface), 0.1);
}

/* 表单圆角 */
.v-text-field :deep(.v-field) {
  border-radius: 12px;
}

.v-btn {
  border-radius: 12px;
}

a:hover {
  text-decoration: underline !important;
}

/* 错误提示 */
.error-message {
  display: flex;
  align-items: center;
  color: #e53935;
  font-size: 14px;
}

/* 密码强度条 */
.strength-segments {
  display: flex;
  align-items: center;
  gap: 3px;
  line-height: 1;
  flex: none;
  white-space: nowrap;
}
.strength-label {
  flex: none;
  min-width: 56px;
  text-align: right;
  margin-right: 6px;
}
.strength-segments .seg {
  flex: none;
  width: 6px;
  height: 0.7em;
  border-radius: 2px;
  background-color: rgba(var(--v-theme-on-surface), 0.12);
  transition: background-color 0.2s ease;
}

/* Logo with slogan */
.logo-with-slogan {
  display: inline-block;
  position: relative;
  margin-bottom: 16px;
}

.logo-slogan {
  position: absolute;
  left: 100%;
  top: 50%;
  transform: translateY(-50%) translateX(-8px);
  margin-left: 16px;
  font-size: 12px;
  color: #999;
  opacity: 0;
  transition:
    opacity 0.3s ease,
    transform 0.3s ease;
  white-space: nowrap;
}

.logo-slogan.visible {
  opacity: 1;
  transform: translateY(-50%) translateX(0);
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Responsive */
@media (max-width: 960px) {
  .intro-section {
    text-align: center;
    padding: 20px;
  }
}
</style>
