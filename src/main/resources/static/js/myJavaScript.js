 $(window).load(function() {
     $(".loading").fadeOut()
 })

 /****/
 $(document).ready(function() {
     var whei = $(window).width()
     $("html").css({ fontSize: whei / 20 })
     $(window).resize(function() {
         var whei = $(window).width()
         $("html").css({ fontSize: whei / 20 })
     });
 });

 //格式化显示实时时间
 $(function() {
     Date.prototype.format = function(fmt) {
         var o = {
             "y+": this.getFullYear, //年
             "M+": this.getMonth() + 1, //月份
             "d+": this.getDate(), //日
             "h+": this.getHours(), //小时
             "m+": this.getMinutes(), //分
             "s+": this.getSeconds() //秒
         };
         if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
         for (var k in o)
             if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
         return fmt;
     }
     setInterval("$('#datetime').html((new Date()).format('yyyy-MM-dd hh:mm:ss'))", 1000);
 })

 //显示实时折线变化曲线
 //实时客流量
 var connectFlowRate = echarts.init(document.getElementById('actualConnect'));
 var yAxisData1 = [];
 for (i = 1; i < 501; i++) {
     yAxisData1.push(null);
 }
 var connectFlowRateOption = {
     animation: false,
     title: {
         text: '实时连接量',
         textStyle: {
             color: '#fff',
             fontWeight: 'normal',
             fontSize: '13',
         }
     },
     tooltip: {
         trigger: 'axis',
         axisPointer: { type: 'cross' }
     },
     grid: {
         top: 15,
         left: 3,
         right: 10,
         bottom: 10
     },
     xAxis: {
         boundaryGap: false,
         data: 500
     },
     yAxis: {
         boundaryGap: false,
         data: 1000
     },
     series: {
         symbol: "none",
         /*去掉小圆点*/
         type: 'line',
         data: yAxisData1,
         smooth: true, //显示为平滑的曲线*/
         itemStyle: {
             normal: {
                 lineStyle: {
                     color: '#00FF00'
                 }
             }
         },
     }
 };

 setInterval("actualC()", 100);

 function actualC() {
     yAxisData1.push(0);
     yAxisData1.shift();
     connectFlowRate.setOption(connectFlowRateOption);
 }

 //实时发布量
 var publishFlowRate = echarts.init(document.getElementById('actualPublish'));
 var yAxisData2 = [];
 for (i = 1; i < 501; i++) {
     yAxisData2.push(null);
 }
 var publishFlowRateOption = {
     animation: false,
     title: {
         text: '实时发布量',
         textStyle: {
             color: '#fff',
             fontWeight: 'normal',
             fontSize: '13',
         }
     },
     tooltip: {
         trigger: 'axis',
         axisPointer: { type: 'cross' }
     },
     grid: {
         top: 15,
         left: 3,
         right: 10,
         bottom: 10
     },
     xAxis: {
         boundaryGap: false,
         data: 500
     },
     yAxis: {
         boundaryGap: false,
         data: 1000
     },
     series: {
         symbol: "none",
         /*去掉小圆点*/
         type: 'line',
         data: yAxisData2,
         smooth: true, //显示为平滑的曲线*/
         itemStyle: {
             normal: {
                 lineStyle: {
                     color: '#FFFFCC'
                 }
             }
         },
     }
 };

 setInterval("actualP()", 100);

 function actualP() {
     yAxisData2.push(0);
     yAxisData2.shift();
     publishFlowRate.setOption(publishFlowRateOption);
 }

 //发布占比
 var myChart1 = echarts.init(document.getElementById('echarts1'));
 var myChart2 = echarts.init(document.getElementById('echarts2'));
 var myChart3 = echarts.init(document.getElementById('echarts3'));

 //主题图谱
 var myChart4 = echarts.init(document.getElementById('echarts4'));

 function getDatas(plantCap) {
     var datas = [];
     for (var i = 0; i < plantCap.length; i++) {
         var offset = [Math.round(Math.random() * 100), Math.round(Math.random() * 100)];
         var symbolSize = Math.round(Math.random() * 80 + 20);
         var item = plantCap[i];
         datas.push({
             name: item.value + '\n' + item.name,
             value: offset,
             symbolSize: symbolSize,
             label: {
                 normal: {
                     textStyle: {
                         fontSize: 14
                     }
                 }
             },

             itemStyle: {
                 normal: {
                     color: 'rgba(73,188,247,.14)'
                 }
             },
         })
     }
     return datas;
 }

 //客户端订阅主题量
 var myChart5 = echarts.init(document.getElementById('websocketTopics'));

 $(document).ready(function() {
     myChart1.resize();
     myChart2.resize();
     myChart3.resize();
     myChart4.resize();
     myChart5.resize();
     connectFlowRate.resize();
     publishFlowRate.resize();
 })
 window.addEventListener("resize", function() {
     myChart1.resize();
     myChart2.resize();
     myChart3.resize();
     myChart4.resize();
     myChart5.resize();
     connectFlowRate.resize();
     publishFlowRate.resize();
 });