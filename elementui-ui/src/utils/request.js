import axios from 'axios'
import {Message} from 'element-ui'

const BASE_URL = 'http://localhost:9999'

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'
const service = axios.create({
    baseURL: BASE_URL,
    timeout: 60000
})

// request拦截器
service.interceptors.request.use(config => {
    return config
}, error => {
    console.log(error)
})

// 响应拦截器
service.interceptors.response.use(res => {
        const code = res.data.code || 200;
        if (res.request.responseType === 'blob' || res.request.responseType === 'arraybuffer') {
            return res.data
        }
        const msg = res.data.message;
        if (code === 500) {
            Message.error(msg)
            return Promise.reject(new Error(msg))
        }
        return res.data
    },
    error => {
        console.log(error)
        Message.error("网络异常")
        return Promise.reject(error)
    }
)

const getAction = (url, params) => {
    return service({
        url,
        method: 'GET',
        params
    })
}

const postAction = (url, data) => {
    return service({
        url,
        method: 'POST',
        data
    })
}

export default {BASE_URL, service, getAction, postAction}
