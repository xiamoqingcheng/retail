import { defineStore } from "pinia";
import { UserState } from "@/stores/interface";
import piniaPersistConfig from "@/stores/helper/persist";

export const useUserStore = defineStore({
  id: "retail-user",
  state: (): UserState => ({
    token: "",
    userInfo: { id: 0, name: "", username: "", role: "" }
  }),
  getters: {},
  actions: {
    setToken(token: string) {
      this.token = token;
    },
    setUserInfo(userInfo: UserState["userInfo"]) {
      this.userInfo = userInfo;
    }
  },
  persist: piniaPersistConfig("retail-user")
});
