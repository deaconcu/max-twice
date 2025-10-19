<template>
  <footer class="app-footer">
    <v-container>
      <div class="footer-content">
        <div class="footer-brand">
          <span class="brand-max">Max</span><span class="brand-twice">Twice</span>
        </div>
        <div class="footer-links">
          <a href="#">关于</a>
          <a href="#">帮助</a>
          <a href="#">隐私</a>
        </div>
        <div class="footer-copyright">© 2025</div>
      </div>
    </v-container>
  </footer>
</template>

<script setup lang="ts">
</script>

<style scoped>
.app-footer {
  background-color: transparent;
  padding: 60px 0;
  margin-top: 120px;
  position: relative;
  z-index: 1;
}

.footer-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48px;
}

.footer-brand {
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.brand-max {
  color: rgb(var(--v-theme-primary));
}

.brand-twice {
  color: #1A1A1B;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 24px;
}

.footer-links a {
  color: #666;
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all 0.2s;
  position: relative;
}

.footer-links a:hover {
  color: rgb(var(--v-theme-primary));
}

.footer-links a::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 1px;
  background: rgb(var(--v-theme-primary));
  transition: width 0.3s ease;
}

.footer-links a:hover::after {
  width: 100%;
}

.footer-copyright {
  color: #ccc;
  font-size: 0.75rem;
  letter-spacing: 0.05em;
}

@media (max-width: 768px) {
  .footer-content {
    flex-direction: column;
    gap: 20px;
  }
}
</style>
