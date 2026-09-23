import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * Interceptor funcional para manejo global de errores HTTP.
 * - 401 → Redirige a login (token expirado o inválido)
 * - 403 → Redirige a home (sin permisos)
 * - 500+ → Log de error en consola
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (error.status) {
        case 401:
          localStorage.removeItem('jwt_token');
          localStorage.removeItem('user_data');
          router.navigate(['/auth/login']);
          break;
        case 403:
          router.navigate(['/']);
          break;
        case 0:
          console.error('[FutPredictionPro] Error de conexión: el servidor no está disponible.');
          break;
        default:
          console.error(`[FutPredictionPro] Error HTTP ${error.status}:`, error.message);
      }
      return throwError(() => error);
    })
  );
};
