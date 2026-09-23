import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Interceptor funcional que inyecta el JWT Bearer token
 * en cada request HTTP saliente.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('jwt_token');

  if (token) {
    const clonedReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(clonedReq);
  }

  return next(req);
};
