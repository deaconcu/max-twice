let ids = [];

function add(m) {
    ids.splice(-1,0,m);
    alert(ids);
}

function refresh() {
    var Ajax = {
        get: function (url, fn) {
            var xhr = new XMLHttpRequest();
            xhr.open('GET', url, true);
            xhr.setRequestHeader('Content-Type','application/x-www-form-urlencode;charset=utf-8');
            // 每当readyState改变时就会触发onreadystatechange函数
            // 0: 请求未初始化
            // 1: 服务器连接已建立
            // 2: 请求已接收
            // 3: 请求处理中
            // 4: 请求已完成，且响应已就绪
            xhr.onreadystatechange = function () {
                if (xhr.readyState === 4 && xhr.status === 200) {
                    fn.call(this, xhr.responseText)
                }
            };
            xhr.send()
        },
        post: function (url, data, fn) {
            var xhr = new XMLHttpRequest();
            xhr.open("POST", url, true);
            // 添加http头，发送信息至服务器时内容编码类型
            xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            xhr.onreadystatechange = function() {
                if (xhr.readyState == 4 && (xhr.status == 200 || xhr.status == 304)) {
                    fn.call(this, xhr.responseText);
                }
            };
            //发送数据
            xhr.send(data);
        }
    }
    // http://192.168.107.230:8608/emu
    Ajax.get('/emu/mf/resourceinfo.do?method=getMapIp', getDatas)
    var bmapcfg = {};
    function getDatas (val) {
        let resData = JSON.parse(val);
        console.log('///', resData);
    }
}



