import { createStore } from 'vuex'
import user from './modules/user'
import cropData from './modules/cropData'
import prediction from './modules/prediction'

export default createStore({
  modules: {
    user,
    cropData,
    prediction
  }
})
