package cuponmaker.smtc.domain.user.controller;

import cuponmaker.smtc.domain.user.dto.LoginRequest;
import cuponmaker.smtc.domain.user.dto.SignupRequest;
import cuponmaker.smtc.domain.user.service.AuthService;
import cuponmaker.smtc.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthPageController {

    private final AuthService authService;

    @GetMapping("/")
    public String home(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        if (userDetails != null) {
            model.addAttribute("isLogin", true);
            model.addAttribute("email", userDetails.getEmail());
            model.addAttribute("nickname", userDetails.getNickname());
        } else {
            model.addAttribute("isLogin", false);
        }

        return "index";
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("signupRequest", new SignupRequest("", "", ""));
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute SignupRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            authService.signup(request);
            return "redirect:/login?signupSuccess";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "signup";
        }
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest("", ""));
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute LoginRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "login";
        }

        try {
            authService.login(request, httpRequest);
            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "이메일 또는 비밀번호가 올바르지 않습니다.");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        authService.logout(request);
        return "redirect:/login?logout";
    }
}
