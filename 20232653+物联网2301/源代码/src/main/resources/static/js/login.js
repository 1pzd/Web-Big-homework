/* 登录页 */
(function () {
    const form = document.getElementById('loginForm');
    const errBox = document.getElementById('errBox');
    const submitBtn = document.getElementById('submitBtn');

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        errBox.classList.remove('show');

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;
        if (!username || !password) {
            errBox.textContent = '请输入用户名和密码';
            errBox.classList.add('show');
            return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = '登录中…';
        try {
            const data = await App.api('/api/auth/login', {
                method: 'POST',
                body: { username: username, password: password }
            });
            App.toast('登录成功，正在进入…', 'success');
            setTimeout(function () {
                location.href = '/chat';
            }, 500);
        } catch (err) {
            errBox.textContent = err.message;
            errBox.classList.add('show');
            submitBtn.disabled = false;
            submitBtn.textContent = '登 录';
        }
    });
})();