/* 注册页 */
(function () {
    const form = document.getElementById('registerForm');
    const errBox = document.getElementById('errBox');
    const submitBtn = document.getElementById('submitBtn');

    function fail(message) {
        errBox.textContent = message;
        errBox.classList.add('show');
        submitBtn.disabled = false;
        submitBtn.textContent = '注 册';
    }

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        errBox.classList.remove('show');

        const payload = {
            username: document.getElementById('username').value.trim(),
            nickname: document.getElementById('nickname').value.trim(),
            email: document.getElementById('email').value.trim(),
            password: document.getElementById('password').value,
            confirmPassword: document.getElementById('confirmPassword').value
        };

        if (payload.password !== payload.confirmPassword) {
            fail('两次输入的密码不一致');
            return;
        }

        submitBtn.disabled = true;
        submitBtn.textContent = '提交中…';
        try {
            await App.api('/api/auth/register', { method: 'POST', body: payload });
            App.toast('注册成功，正在跳转登录页…', 'success');
            setTimeout(function () {
                location.href = '/login';
            }, 800);
        } catch (err) {
            fail(err.message);
        }
    });
})();