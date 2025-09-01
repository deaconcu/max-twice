<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter} from 'vue-router';
import { useI18n } from 'vue-i18n';
import { courseServiceV1 } from '@/services/api/v1/apiServiceV1';

const { t } = useI18n();
//const isLoggedIn = ref(false);
const route = useRoute();
const router = useRouter();

const sendMessage = () => {
  alert("search");
}

const course = ref([]);

onMounted(async () => {
  try {
    const response = await courseServiceV1.getCourse(route.params.id);

    if (response.code === 401) {
      console.log('not login');
      //router.push('/login');
    } else if (response.code === 200) {
      console.log('get data:' + JSON.stringify(response.data));
      course.value = response.data;
    } 
  } catch (error) {
    console.error('Error verifying login status:', error);
    isLoggedIn.value = false; // 如果请求失败，认为用户未登录
  }
});

const relatedLinks = ['高等数学', '概率论', 'C++编程实现', '软件测试']
</script>

<template>
  <Suspense>
    <v-container fluid>
      <v-row no-gutters class="mt-8">
        <v-col cols=9 class="pr-16">
          <!--
          <div class="d-flex align-center">
            <v-icon size="22">mdi-format-list-bulleted</v-icon>
            <span class="text-h6 ml-3 font-weight-black text">课程</span>
          </div>
          <div>
            <img src=""></img>
          </div>-->
          <div>
            <v-row no-gutters justify="center" class="ma-0">
              <v-col class="text-h6" cols="9">{{ course.name }}</v-col>
              <v-col cols="3" class="d-flex justify-end">
                <router-link to="/" class="px-0 mr-5">{{ t('course.subscribe') }}</router-link>
                <router-link :to="`/read?courseId=${course.id}&path=1-${course.rootNode}`" class="px-0">{{ t('course.startLearning') }}</router-link>
              </v-col>
            </v-row>
            <v-row class="mx-0 mt-5 mb-12">
              {{ course.description}}
            </v-row>

            <v-row no-gutters justify="space-between" align="center"  class="mb-0">
              <div class="mb-0 ">{{ t('course.subcourses') }}</div>
              <div>
                <v-btn variant="plain" density="comfortable" icon="mdi-plus-circle-outline" :ripple="false"></v-btn>
                <v-btn variant="plain" density="comfortable" icon="mdi-information-outline" :ripple="false"></v-btn>
              </div>
            </v-row>
            <v-divider></v-divider>
            <v-row class="mx-0">
            </v-row>
            <span></span>

          </div>
        </v-col>
        <v-col cols=3>
          <v-card
            :disabled="loading"
            :loading="loading"
            class="mx-auto my-0"
            style="border: 1px solid #ddd;"
            variant="outlined"
          >
            <template v-slot:loader="{ isActive }">
              <v-progress-linear
                :active="isActive"
                color="deep-purple"
                height="4"
                indeterminate
              ></v-progress-linear>
            </template>

            <v-img
              height="250"
              src="https://cdn.vuetifyjs.com/images/cards/cooking.png"
              cover
            ></v-img>

            <v-card-item>
              <v-card-title>Cafe Badilico</v-card-title>

              <v-card-subtitle>
                <span class="me-1">Local Favorite</span>

                <v-icon
                  color="error"
                  icon="mdi-fire-circle"
                  size="small"
                ></v-icon>
              </v-card-subtitle>
            </v-card-item>

            <v-card-text>
              <v-row
                align="center"
                class="mx-0"
              >
                <v-rating
                  :model-value="4.5"
                  color="amber"
                  density="compact"
                  size="small"
                  half-increments
                  readonly
                ></v-rating>

                <div class="text-grey ms-4">
                  4.5 (413)
                </div>
              </v-row>

              <div class="my-4 text-subtitle-1">
                $ • Italian, Cafe
              </div>

              <div>Small plates, salads & sandwiches - an intimate setting with 12 indoor seats plus patio seating.</div>
            </v-card-text>
          </v-card>

          <div class="font-weight-bold mt-10">{{ t('course.related') }}</div>
          <v-list lines="one">
            <v-list-item
              v-for="link in relatedLinks"
              :key="n"
              class="px-0 py-1 ma-0"
              min-height="30px"
            ><router-link to="/" class="px-0">{{ link }}</router-link>
            </v-list-item>
          </v-list>

        </v-col>
      </v-row>
    </v-container>
  </Suspense>
</template>

<style>
</style>