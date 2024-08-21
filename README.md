### 테스트 사전조건
- /etc/hosts 파일 domain 등록
  1. 127.0.0.1 a.com
  2. 127.0.0.1 b.com
  3. 127.0.0.1 c.com
  

- windows인 경우 "설치드라이브:\Windows\System32\drivers\etc" 아래

### 실행 방법
1) mvn clean package
2) build_and_run.sh 

### 스펙 구현 여부
1) HTTP/1.1 의 Host 헤더를 해석하세요.


    o 예를 들어, a.com 과 b.com 의 IP 가 같을지라도 설정에 따라 
      서버에서 다른 데이터를 제공할 수 있어야 합니다.
    o 아파치 웹 서버의 VirtualHost 기능을 참고하세요.
        http://a.com:8000
        http://b.com:8000
        http://c.com:8000
    

3) 다음 사항을 설정 파일로 관리하세요.


    o config.json 설정 파일
```
    {
        "port": 8000,
        "httpRoot": "/www",
        "indexPage": "index.html",
        "errorPages": {
            "403": "errors/403.html",
            "404": "errors/404.html",
            "500": "errors/500.html"
        },
        "hosts": {
            "a.com": {
                "HTTP_ROOT": "www/a"
            },
            "b.com": {
                "HTTP_ROOT": "www/b"
            },
            "c.com": {
                "HTTP_ROOT": "www/aa/bb"
            }
        },
        "servlet": {
            "/Greeting": "Hello",
            "/super.Greeting": "service.Hello",
            "/time": "CurrentTimeServlet"
        }
    }
```


4) 403, 404, 500 오류를 처리합니다.


    o comfig.json에 서블릿 mapping 등록 없을시
      정적인 파일 처리로 403/404/500 오류 처리
      /resources/errors/403.html, 404.html, 500.html

5) 다음과 같은 보안 규칙을 둡니다.


    o 다음 규칙에 걸리면 응답 코드 403 을 반환합니다.
      ▪ HTTP_ROOT 디렉터리의 상위 디렉터리에 접근할 때,
        예, http://localhost:8000/../../../../etc/passwd
      -> 상위 디렉터리 접근 불가 403
      ▪ 확장자가 .exe 인 파일을 요청받았을 때
      -> exe체크
    o 추후 규칙을 추가할 것을 고려해주세요.
      ->


6) logback 프레임워크 http://logback.qos.ch/를 이용하여 다음의 로깅 작업을 합니다.


    o 로그 파일을 하루 단위로 분리합니다. 
      -> daily
    o 로그 내용에 따라 적절한 로그 레벨을 적용합니다.
      -> debug, info, warn, error
    o 오류 발생 시, StackTrace 전체를 로그 파일에 남깁니다.
      -> 

7) 간단한 WAS 를 구현합니다.

   http://localhost:8000/Greeting?name=홍길동
   http://localhost:8000/super.Greeting?name=HoHO


7) 현재 시각을 출력하는 SimpleServlet 구현체를 작성하세요.

   http://localhost:8000/time


8) 앞에서 구현한 여러 스펙을 검증하는 테스트 케이스를 JUnit4 를 이용해서 작성하세요.



## war 환경에서 정적파일 접근시 403 발생