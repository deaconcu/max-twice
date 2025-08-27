<script setup>
import { useI18n } from 'vue-i18n'
import WelcomeItem from './WelcomeItem.vue'
import DocumentationIcon from './icons/IconDocumentation.vue'
import ToolingIcon from './icons/IconTooling.vue'
import EcosystemIcon from './icons/IconEcosystem.vue'
import CommunityIcon from './icons/IconCommunity.vue'
import SupportIcon from './icons/IconSupport.vue'

const { t } = useI18n()
const openReadmeInEditor = () => fetch('/__open-in-editor?file=README.md')
</script>

<template>
  <WelcomeItem>
    <template #icon>
      <DocumentationIcon />
    </template>
    <template #heading>{{ t('welcome.documentation') }}</template>

    {{ t('welcome.vueDocumentation') }}
    <a href="https://vuejs.org/" target="_blank" rel="noopener">{{ t('welcome.officialDocumentation') }}</a>
    {{ t('welcome.providesInformation') }}
  </WelcomeItem>

  <WelcomeItem>
    <template #icon>
      <ToolingIcon />
    </template>
    <template #heading>{{ t('welcome.tooling') }}</template>

    {{ t('welcome.toolingDescription1') }}
    <a href="https://vite.dev/guide/features.html" target="_blank" rel="noopener">Vite</a>{{ t('welcome.toolingDescription2') }}
    <a href="https://code.visualstudio.com/" target="_blank" rel="noopener">VSCode</a>
    +
    <a href="https://github.com/johnsoncodehk/volar" target="_blank" rel="noopener">Volar</a>{{ t('welcome.toolingDescription3') }}
    <a href="https://vitest.dev/" target="_blank" rel="noopener">Vitest</a>
    {{ t('welcome.and') }}
    <a href="https://www.cypress.io/" target="_blank" rel="noopener">Cypress</a>
    /
    <a href="https://playwright.dev/" target="_blank" rel="noopener">Playwright</a>.

    <br />

    {{ t('welcome.moreInstructions') }}
    <a href="javascript:void(0)" @click="openReadmeInEditor"><code>README.md</code></a>.
  </WelcomeItem>

  <WelcomeItem>
    <template #icon>
      <EcosystemIcon />
    </template>
    <template #heading>{{ t('welcome.ecosystem') }}</template>

    {{ t('welcome.ecosystemDescription') }}
    <a href="https://pinia.vuejs.org/" target="_blank" rel="noopener">Pinia</a>,
    <a href="https://router.vuejs.org/" target="_blank" rel="noopener">Vue Router</a>,
    <a href="https://test-utils.vuejs.org/" target="_blank" rel="noopener">Vue Test Utils</a>, {{ t('welcome.and') }}
    <a href="https://github.com/vuejs/devtools" target="_blank" rel="noopener">Vue Dev Tools</a>{{ t('welcome.ecosystemDescription2') }}
    <a href="https://github.com/vuejs/awesome-vue" target="_blank" rel="noopener">Awesome Vue</a>
    {{ t('welcome.aVisit') }}
  </WelcomeItem>

  <WelcomeItem>
    <template #icon>
      <CommunityIcon />
    </template>
    <template #heading>{{ t('welcome.community') }}</template>

    {{ t('welcome.communityDescription') }}
    <a href="https://chat.vuejs.org" target="_blank" rel="noopener">Vue Land</a>
    {{ t('welcome.discordServer') }}
    <a href="https://stackoverflow.com/questions/tagged/vue.js" target="_blank" rel="noopener"
      >StackOverflow</a
    >{{ t('welcome.communityDescription2') }}
    <a href="https://bsky.app/profile/vuejs.org" target="_blank" rel="noopener">@vuejs.org</a>
    {{ t('welcome.blueskyAccount') }}
    <a href="https://x.com/vuejs" target="_blank" rel="noopener">@vuejs</a>
    {{ t('welcome.xAccount') }}
  </WelcomeItem>

  <WelcomeItem>
    <template #icon>
      <SupportIcon />
    </template>
    <template #heading>{{ t('welcome.supportVue') }}</template>

    {{ t('welcome.supportDescription') }}
    <a href="https://vuejs.org/sponsor/" target="_blank" rel="noopener">{{ t('welcome.becomingSponsor') }}</a>.
  </WelcomeItem>
</template>
