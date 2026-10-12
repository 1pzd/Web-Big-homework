/* 后台数据看板：拉取统计数据并渲染 ECharts 图表 */
(function () {

    const PALETTE = ['#5b5bf5', '#8b5cf6', '#22c55e', '#f59e0b', '#06b6d4', '#ec4899'];

    function hasEcharts() {
        return typeof echarts !== 'undefined';
    }

    function chartFallback(id, message) {
        const el = document.getElementById(id);
        if (el) {
            el.innerHTML = '<div style="height:100%;display:grid;place-items:center;color:#8a90a8;'
                + 'font-size:13px;text-align:center;padding:20px;">' + message + '</div>';
        }
    }

    function renderStats(data) {
        const rate = data.hitRate || 0;
        const satisfied = data.satisfiedRate || 0;
        const cards = [
            { label: '注册用户', value: data.userCount, extra: '平台累计注册账号' },
            { label: '问答会话', value: data.sessionCount, extra: '累计对话轮次' },
            { label: '提问总数', value: data.messageCount, extra: '用户提问条数' },
            { label: '知识条目', value: data.knowledgeCount, extra: '知识库标准问答' },
            { label: '今日提问', value: data.todayMessageCount, extra: '当天 00:00 起统计' },
            { label: '知识库命中率', value: Math.round(rate * 100) + '%', extra: '命中知识库的答案占比' },
            { label: '答案满意度', value: Math.round(satisfied * 100) + '%', extra: '有帮助 / 已评价' }
        ];
        document.getElementById('statGrid').innerHTML = cards.map(function (c) {
            return '<div class="stat-card"><div class="label">' + c.label + '</div>'
                + '<div class="value">' + (c.value === null || c.value === undefined ? 0 : c.value) + '</div>'
                + '<div class="extra">' + c.extra + '</div></div>';
        }).join('');
    }

    function renderTables(data) {
        const hot = data.hotKnowledge || [];
        document.getElementById('hotKnowledgeBody').innerHTML = hot.length === 0
            ? '<tr><td colspan="3" style="text-align:center;color:#8a90a8;padding:24px;">暂无数据</td></tr>'
            : hot.map(function (row, i) {
                return '<tr><td>' + (i + 1) + '</td>'
                    + '<td class="cell-ellipsis">' + App.esc(row.question) + '</td>'
                    + '<td>' + row.hitCount + '</td></tr>';
            }).join('');

        const questions = data.hotQuestions || [];
        document.getElementById('hotQuestionBody').innerHTML = questions.length === 0
            ? '<tr><td colspan="3" style="text-align:center;color:#8a90a8;padding:24px;">暂无数据</td></tr>'
            : questions.map(function (row, i) {
                return '<tr><td>' + (i + 1) + '</td>'
                    + '<td class="cell-ellipsis">' + App.esc(row.question) + '</td>'
                    + '<td>' + row.num + '</td></tr>';
            }).join('');
    }

    function renderCharts(data) {
        if (!hasEcharts()) {
            ['chartTrend', 'chartSource', 'chartCategory', 'chartHour'].forEach(function (id) {
                chartFallback(id, '图表库 ECharts 未能从 CDN 加载，请检查网络连接');
            });
            return;
        }

        const baseGrid = { left: 44, right: 20, top: 30, bottom: 30 };

        // 1. 提问趋势
        const trend = data.trend || [];
        echarts.init(document.getElementById('chartTrend')).setOption({
            color: PALETTE,
            tooltip: { trigger: 'axis' },
            grid: baseGrid,
            xAxis: {
                type: 'category',
                data: trend.map(function (r) { return r.day; }),
                boundaryGap: false,
                axisLine: { lineStyle: { color: '#e6e9f5' } },
                axisLabel: { color: '#8a90a8' }
            },
            yAxis: {
                type: 'value',
                minInterval: 1,
                splitLine: { lineStyle: { color: '#f1f3fa' } },
                axisLabel: { color: '#8a90a8' }
            },
            series: [{
                type: 'line',
                smooth: true,
                symbolSize: 7,
                data: trend.map(function (r) { return r.num; }),
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        { offset: 0, color: 'rgba(91,91,245,.32)' },
                        { offset: 1, color: 'rgba(91,91,245,0)' }
                    ])
                },
                lineStyle: { width: 3 }
            }]
        });

        // 2. 答案来源占比
        const sourceNames = { knowledge: '知识库命中', llm: '大模型生成', fallback: '未命中' };
        const sourceStats = (data.sourceStats || []).map(function (r) {
            return { name: sourceNames[r.source] || r.source || '未知', value: r.num };
        });
        echarts.init(document.getElementById('chartSource')).setOption({
            color: PALETTE,
            tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
            legend: { bottom: 0, textStyle: { color: '#4b5169' } },
            series: [{
                type: 'pie',
                radius: ['46%', '70%'],
                center: ['50%', '45%'],
                avoidLabelOverlap: true,
                itemStyle: { borderColor: '#fff', borderWidth: 2 },
                label: { formatter: '{b}\n{d}%', color: '#4b5169', fontSize: 12 },
                data: sourceStats.length === 0 ? [{ name: '暂无数据', value: 1 }] : sourceStats
            }]
        });

        // 3. 分类知识量
        const categoryStats = data.categoryStats || [];
        echarts.init(document.getElementById('chartCategory')).setOption({
            color: PALETTE,
            tooltip: { trigger: 'axis' },
            grid: baseGrid,
            xAxis: {
                type: 'category',
                data: categoryStats.map(function (r) { return r.name; }),
                axisLine: { lineStyle: { color: '#e6e9f5' } },
                axisLabel: { color: '#8a90a8', interval: 0, fontSize: 11 }
            },
            yAxis: {
                type: 'value',
                minInterval: 1,
                splitLine: { lineStyle: { color: '#f1f3fa' } },
                axisLabel: { color: '#8a90a8' }
            },
            series: [{
                type: 'bar',
                barWidth: '46%',
                itemStyle: { borderRadius: [8, 8, 0, 0] },
                data: categoryStats.map(function (r) { return r.num; })
            }]
        });

        // 4. 活跃时段
        const hourMap = {};
        (data.hourStats || []).forEach(function (r) {
            hourMap[Number(r.hour)] = Number(r.num);
        });
        const hours = [];
        const hourValues = [];
        for (let h = 0; h < 24; h++) {
            hours.push(h + '时');
            hourValues.push(hourMap[h] || 0);
        }
        echarts.init(document.getElementById('chartHour')).setOption({
            color: ['#8b5cf6'],
            tooltip: { trigger: 'axis' },
            grid: baseGrid,
            xAxis: {
                type: 'category',
                data: hours,
                axisLine: { lineStyle: { color: '#e6e9f5' } },
                axisLabel: { color: '#8a90a8', interval: 2, fontSize: 11 }
            },
            yAxis: {
                type: 'value',
                minInterval: 1,
                splitLine: { lineStyle: { color: '#f1f3fa' } },
                axisLabel: { color: '#8a90a8' }
            },
            series: [{
                type: 'bar',
                barWidth: '58%',
                itemStyle: { borderRadius: [6, 6, 0, 0] },
                data: hourValues
            }]
        });

        window.addEventListener('resize', function () {
            ['chartTrend', 'chartSource', 'chartCategory', 'chartHour'].forEach(function (id) {
                const instance = echarts.getInstanceByDom(document.getElementById(id));
                if (instance) instance.resize();
            });
        });
    }

    (async function init() {
        const user = await AdminLayout.init('dashboard', '数据看板');
        if (!user) return;
        try {
            const data = await App.api('/api/admin/stats/dashboard');
            renderStats(data);
            renderTables(data);
            renderCharts(data);
        } catch (e) {
            App.toast(e.message, 'error');
        }
    })();
})();