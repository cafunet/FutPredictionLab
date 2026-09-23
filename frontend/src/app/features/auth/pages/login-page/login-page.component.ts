import { Component, inject, signal, OnInit, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

declare var google: any;

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css'
})
export class LoginPageComponent implements AfterViewInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  @ViewChild('googleBtnRef') googleBtnRef!: ElementRef;

  loginError = signal<string | null>(null);
  showPassword = signal<boolean>(false);

  ngAfterViewInit(): void {
    if (typeof google !== 'undefined' && google.accounts) {
      google.accounts.id.initialize({
        client_id: '182599824431-lipjlu39nre94psjf111718lnqvbl6u8.apps.googleusercontent.com', // <-- Reemplazar con el Client ID real
        callback: this.handleGoogleCredentialResponse.bind(this)
      });
      google.accounts.id.renderButton(
        this.googleBtnRef.nativeElement,
        { theme: 'outline', size: 'large', width: 300, shape: 'pill' } 
      );
    }
  }

  handleGoogleCredentialResponse(response: any) {
    if (response.credential) {
      this.loginError.set(null);
      this.authService.loginWithGoogle(response.credential).subscribe({
        next: () => {
          if (this.authService.isAdmin()) {
            this.router.navigate(['/admin']);
          } else {
            this.router.navigate(['/']);
          }
        },
        error: (err) => {
          this.loginError.set(err.error?.message ?? 'Error al iniciar sesión con Google.');
        }
      });
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }

  doLogin(email: string, password: string): void {
    if (!email || !password) {
      this.loginError.set('Faltan credenciales.');
      return;
    }

    this.loginError.set(null);
    this.authService.login({ email, password }).subscribe({
      next: () => {
        if (this.authService.isAdmin()) {
          this.router.navigate(['/admin']);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err) => {
        this.loginError.set(err.error?.message ?? 'Credenciales inválidas. Intenta de nuevo.');
      }
    });
  }
}

