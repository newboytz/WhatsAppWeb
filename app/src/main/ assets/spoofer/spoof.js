const code = `
(function() {
    // 1. Weka sifa za Windows 11 PC
    Object.defineProperty(navigator, 'platform', { get: () => 'Win32' });
    Object.defineProperty(navigator, 'oscpu', { get: () => 'Windows NT 10.0; Win64; x64' });
    Object.defineProperty(navigator, 'maxTouchPoints', { get: () => 0 });
    delete window.ontouchstart;

    // 2. Danganya vipimo vya skrini ya Monitor ya PC
    Object.defineProperty(screen, 'width', { get: () => 1920 });
    Object.defineProperty(screen, 'height', { get: () => 1080 });
    Object.defineProperty(screen, 'availWidth', { get: () => 1920 });
    Object.defineProperty(screen, 'availHeight', { get: () => 1040 });

    // 3. Ficha GPU ya simu (Adreno/Mali) ionekane ni Intel PC
    const getParameter = WebGLRenderingContext.prototype.getParameter;
    WebGLRenderingContext.prototype.getParameter = function(parameter) {
        if (parameter === 37445) return 'Intel Inc.';
        if (parameter === 37446) return 'Intel(R) UHD Graphics 630';
        return getParameter.apply(this, arguments);
    };

    // 4. Rekebisha muonekano wa WhatsApp Web kwenye skrini ndogo (CSS fix)
    window.addEventListener('DOMContentLoaded', () => {
        try {
            var css = '.two > div:nth-child(4){flex: 1 0 100vmin;}.two{overflow:visible}';
            var style = document.createElement('style');
            style.textContent = css;
            (document.head || document.documentElement).appendChild(style);
        } catch(e) {}
    });
})();
`;

const script = document.createElement('script');
script.textContent = code;
(document.head || document.documentElement).appendChild(script);
script.remove();
  
