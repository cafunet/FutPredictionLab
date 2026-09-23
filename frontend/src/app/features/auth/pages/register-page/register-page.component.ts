import { Component, inject, signal, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

declare var google: any;

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.css'
})
export class RegisterPageComponent implements AfterViewInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  @ViewChild('googleBtnRef') googleBtnRef!: ElementRef;

  registerError = signal<string | null>(null);
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
      this.registerError.set(null);
      this.authService.loginWithGoogle(response.credential).subscribe({
        next: () => {
          if (this.authService.isAdmin()) {
            this.router.navigate(['/admin']);
          } else {
            this.navigateAfterAuth();
          }
        },
        error: (err) => {
          this.registerError.set(err.error?.message ?? 'Error al registrarse con Google.');
        }
      });
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }

  doRegister(fullName: string, email: string, password: string): void {
    if (!fullName || !email || !password) {
      this.registerError.set('Completa todos los campos, por favor.');
      return;
    }

    this.registerError.set(null);
    this.authService.register({ fullName, email, password }).subscribe({
      next: () => {
        this.navigateAfterAuth();
      },
      error: (err) => {
        this.registerError.set(err.error?.message ?? 'Error al registrar. Intenta de nuevo.');
      }
    });
  }

  private navigateAfterAuth(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/');
  }
}
