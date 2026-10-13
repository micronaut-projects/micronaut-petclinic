(() => {
    const loginPath = "/user/auth";
    const nativeFetch = window.fetch.bind(window);

    window.fetch = (...args) => nativeFetch(...args).then((response) => {
        if (response.status === 401 && window.location.pathname !== loginPath) {
            window.petclinicAuthRedirecting = true;
            window.location.replace(loginPath);
        }
        return response;
    });
})();
