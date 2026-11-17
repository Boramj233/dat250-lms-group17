# Frontend-review – DAT250 LMS (gruppe 17)

*Dato: 2026-11-16. Omfang: `frontend/` (≈13 300 linjer i `src/`, 186 filer). Alle stier er relative til `frontend/src/` med mindre annet står. Ingen kode er endret.*

> **Status (oppdatert 2026-11-16).** Resten av dokumentet beskriver koden *før* endringene, så filstier i det kan være utdaterte.
>
> **Gjort:**
>
> - `domain/` + `features/` er skilt, med path-aliaser (`@app`, `@shared`, `@domain`, `@features`). Dette er trinn 2 og 8.
> - Importreglene og `import/no-cycle` håndheves av oxlint. Dette er trinn 0 og 3.
> - `course-view` er ryddet: `content/`, `submissions/`, `DetailLayout` og oppdelte store filer. Dette er trinn 4.
> - Gjenbruk er innført: `useInvalidatingMutation`, `useQueriesById`, `errorMessage`, `courseItems`/`useCourseItems` og `isSubmittable`. Dette er kandidat 1, 2, 3, 5 og 7.
> - Gjentatt CSS er fjernet: fokusring, `.text-meta`, `.tag`, `.disclosure-chevron` og tokens. Dette er kandidat 9.
> - Rydding: `Pager` og interne eksporter er fjernet, utdaterte kommentarer er rettet, og Prettier er innført og kjørt på hele `frontend/`. Dette er trinn 1.
> - `course-management` er ryddet:
>   - `ModuleListPage` har fått nytt navn.
>   - `ContentForm` og `VisibleFromField` ligger i `module/components/`.
>   - `ManageModulePage` og `ContentForm` er delt opp.
>   - Kladdelogikken og skjemalogikken er testet.
> - Rolleetiketter og sortering av medlemmer ligger samlet i `domain/courses/members.ts`.
> - Testene kan importere alle `.ts`-filer med aliaser (`test/setup/`).
>
> **Gjenstår:**
>
> - `ErrorBoundary`, rutevakt for lærersider og `useMatch`-mønstre i `paths.ts` (robusthet).
> - `useDraftSave`: lagringsløkken i `ManageModulePage` og `CourseMembersPage` er fortsatt ikke testet.
> - Kandidat 4, 6, 8, 10–17.
> - Komponenttester.

## 1. Sammendrag

1. **Strukturen er bedre enn den føles.** Den er allerede feature-basert, har dokumenterte regler (`frontend/README.md:58-98`) og ren lagdeling. `shared/` importerer aldri fra `features/` eller `app/`, ingen feature importerer `app/`, og det finnes ingen sirkulære avhengigheter på filnivå. Det trengs ingen omskriving.
2. **Hovedårsaken til at det føles rotete:** `features/` blander to ulike akser på samme nivå. Den ene er *datadomener* (`assignments`, `content`, `grades`, `groups`, `modules`, `quizzes`, `users`), som bare har api/hooks/types. Den andre er *UI-flater* (`course-view`, `course-management`, `dashboard`, `profile`), som bare har sider. `courses` og `deadlines` er begge deler. Brukergrensesnittet for oppgaver ligger altså i `course-view`, mens dataene ligger i `assignments`. Det står ikke noe sted.
3. **Anbefaling:** gjør aksene eksplisitte. Flytt datadomenene til `src/domain/<domene>/` og la `src/features/` inneholde bare UI-flater (sider og sideinterne deler). Det er i stor grad en `git mv` og gir en enkel regel for hvor ting hører hjemme.
4. **295 imports har tre eller flere `../`** (24 har fem). Path-aliaser (`#shared/…`, `#domain/…`) fjerner dette og gjør flyttinger billige. Gjør dette først.
5. **`course-view` er for stor og uoversiktlig.** Den har 4 835 linjer (36 % av koden) og blander studentsider med lærersider (innleveringer og retting), selv om README sier at retting hører hjemme et annet sted. I tillegg ligger byggeklosser som ikke er ruter (`ContentView`, `ContentTypeTable`) i `pages/`, og den generiske `DataTable` ligger i en feature.
6. **De største gevinstene fra gjenbruk er i logikk, ikke i UI-komponenter:**
   - 8 nesten identiske mutasjoner med invalidering
   - 8 kopier av «flat ut emnets innhold på tvers av moduler»
   - 7 håndskrevne funksjoner som gjør `ApiError` om til en melding
   - 5 kopier av `useQueries` + `combine` til `Map`
   - 9 sjekker av typen «oppgave eller kodeoppgave»
7. **Den største UI-gevinsten:** `SubmittableContent` har allerede `Card`, `Layout` og `DeadlineFact`, men `QuizView` og `SubmissionGradingPage` skriver den samme markupen for hånd, rundt 10 kort og 3 paneler.
8. **CSS-en har mye gjentatt kode:**
   - paret `font-ui` + `text-small`: 48 ganger
   - fokusringen: 23 ganger
   - listenullstilling: 14 ganger
   - stilen for store bokstaver: 11 ganger
   - pille-tag med samme form: 8 klasser

   Noen få hjelpeklasser i `shared/styles` fjerner mye av dette.
9. **Reglene håndheves ikke.** `.oxlintrc.json` har ingen regler for importgrenser, og README er delvis utdatert (`frontend/README.md:52-56` sier at studentsidene er fjernet). Grensene kan håndheves med oxlint-regler.
10. **Testene dekker bare ren logikk** (12 `node:test`-filer). Det finnes ingen komponent- eller hook-tester. Før vi rører sidene med kladd- og lagringsflyt (`ManageModulePage`, `CourseMembersPage`) og retting og quiz (`SubmissionGradingPage`, `QuizView`), bør det på plass en sjekkliste for røyktesting mot `npm run dev:mock`. Helst bør det også komme noen få Vitest- og RTL-tester.

## 2. Dagens situasjon

### 2.1 Oppsett
- **Bygg:** Vite 8 (`vite.config.ts`). Proxy mot Spring på `:8080`, eller `mode === 'mock'` med `mock/server.ts`, en falsk backend på 1 786 linjer.
- **TypeScript:** `tsconfig.app.json` med `moduleResolution: bundler` og `allowImportingTsExtensions`. Ingen `paths` og ingen `resolve.alias`: alle imports er relative.
- **Routing:** deklarativ `<Routes>` i `app/routes.tsx` (112 linjer), som importerer 32 sidemapper og `features/course-view/contentSections.ts`. URL-mønstre og lenkebyggere ligger i `shared/paths.ts`.
- **Providers:** `app/main.tsx` setter opp `StrictMode`, deretter `QueryClientProvider`, deretter `BrowserRouter` og til slutt `App`.
- **Lint:** oxlint med bare `rules-of-hooks` og `only-export-components`.
- **Tester:** `node --experimental-strip-types --test test/*.test.mjs`.

### 2.2 Mappestruktur i dag (tre nivåer)
```text
src/
├── app/            main.tsx, App.tsx, routes.tsx, queryClient.ts
│   ├── auth/       AuthGate, RequireAccess, LandingPage, LoginButton
│   └── layout/     AppLayout, Topbar, Sidebar, CoursesPanel
├── shared/
│   ├── api/        apiClient.ts (Bearer, 401-retry, ApiError)
│   ├── auth/       auth.ts, useAuth.ts, access.ts
│   ├── lib/        dateFormat, safeUrl, useDebounced, moveItem
│   ├── navigation/ Trail (breadcrumbs via history-state), trailState, trailContext
│   ├── styles/     primitive.css, semantic.css, global.css, forms.css
│   ├── ui/         ActionMenu, ConfirmDialog, NotFoundPage, PageHeader, Pager, QueryState, RequiredMark, SaveBar, Tabs
│   ├── paths.ts    types.ts
└── features/
    ├── assignments/  content/  grades/  groups/  modules/  quizzes/  users/   ← bare data (api/hooks/types/queryKeys + regler)
    ├── courses/      data + components/{CourseCard,CourseGrid,CourseForm} + pages/CoursesPage
    ├── deadlines/    data + components/DeadlineList
    ├── dashboard/    pages/DashboardPage (+ StudentDashboard, CourseManagerDashboard)
    ├── profile/      pages/ProfilePage
    ├── course-view/  components/ (8), pages/ (18 mapper), module/pages/ (4), courseContext, courseMenu, contentSections, submissionStats, usePeopleNames
    └── course-management/  pages/ (7), module/{pages/ (4), ContentForm/, VisibleFromField.tsx, useManagedContent.ts}, fileTypes, roleLabels, useManageBreadcrumbs, manageCourseContext
```

**Organiseringsprinsipp i praksis:** feature-basert med en delt `shared/`, slik `frontend/README.md:3-5` beskriver. Inne i hver feature er filene sortert etter type (`pages/`, `components/`, `api.ts`, `hooks.ts`). Hver komponent har sin egen mappe med `index.ts` (64 barrels), og de brukes konsekvent: 152 imports går via `index.ts`, og ingen går forbi.

### 2.3 Funksjonalitet (ruter gruppert etter domene)
| Domene | Ruter | Feature i dag |
|---|---|---|
| Innlogging | `/login` → `/`; `AuthGate` viser `LandingPage` eller appen | `app/auth` |
| Oversikt | `/` (student- eller kursansvarlig-dashboard), `/courses`, `/profile` | `dashboard`, `courses`, `profile` |
| Emne for medlemmer | `/courses/:id` + `modules`, `assignments`, `quizzes`, `code-exercises`, `people`, `groups`, `grades`, `files`, `…/:contentId` | `course-view` |
| Modul for medlemmer | `/courses/:id/modules/:moduleId` + `pages`, `assignments`, `quizzes`, `code-exercises`, `files`, `…/:contentId` (`CONTENT_SECTIONS.flatMap`, `routes.tsx:76-79`) | `course-view/module` |
| Retting for lærere | `/courses/:id/submissions[/:contentId[/:submissionId]]` | `course-view` (ingen rutevakt, se 2.5) |
| Emneadministrasjon | `/manage-courses`, `/new`, `/:id` (Content-, Members- og Details-faner), `/:id/modules/:moduleId` (+ `new/:type`, `content/:contentId`) | `course-management` (bak `RequireAccess COURSE_MANAGER`) |

### 2.4 Tverrgående mekanismer
| Mekanisme | Slik er det i dag | Vurdering |
|---|---|---|
| Serverdata | React Query. `app/queryClient.ts`: staleTime 5 min, ingen retry ved `AuthenticationRequiredError`. Invalidering ligger i hver features `hooks.ts`. | Bra og konsekvent. Mye boilerplate (se 3). |
| API | `shared/api/apiClient.ts`: `apiPath` tagged template, `jsonBody`, `apiClientOrNull` (404 → null), `apiDownload`. `ApiError` har bare status. | Bra. Alle `api.ts` går via klienten. Ingen dupliserte fetch-wrappere. |
| Auth | Token i minnet via `/token` (`shared/auth/auth.ts`), popup-innlogging, `useCurrentUser`, `access.ts` (rangering). Emneroller (TEACHER/STUDENT) kommer fra `useCourseRole` i `courses/hooks.ts`. | Ryddig. |
| Klient-state | Fire React-contexts levert av layouts: `course-view/courseContext.ts`, `course-view/module/moduleContext.ts`, `course-management/manageCourseContext.ts`, `course-management/module/manageModuleContext.ts`. Ingen global store. | Passer størrelsen. `courseContext.ts` eksporterer også `visibleContent` (logikk i context-filen). |
| Skjema | Ingen skjemabibliotek. Native validering + `RequiredMark`. Kladd og lagring via `shared/ui/SaveBar` + `useLeaveGuard`. | OK. Markupen gjentas (se 3). |
| Lasting og feil | `shared/ui/QueryState` i 13 filer, men 11 steder skriver `<p role="status">Loading…` selv (bl.a. `CourseLayout.tsx:27`, `ManageCourseLayout.tsx:20`, `ModuleLayout.tsx:131`, `CoursesPanel.tsx:69`, `QuizView.tsx:422`). | Bryter README-regelen (`README.md:71`). Delvis fordi `QueryState` ikke støtter flere spørringer eller `useQueries`. |
| Feilgrenser | Ingen `ErrorBoundary` eller `errorElement`. | En feil i rendering gir hvit side. |
| Navigasjon | To systemer for brødsmuler: Trail (`shared/navigation`) og URL-baserte (`ModuleLayout.tsx:38-54`, `course-management/useManageBreadcrumbs.ts`). `useMatch`-mønstre er hardkodet fire steder (`CourseLayout.tsx:22-24`, `CourseMenuLayout.tsx:21`, `ModuleLayout.tsx:26-27`, `useManageBreadcrumbs.ts:15-17`) og dupliserer `paths.ts`. | Lenker går konsekvent via `paths.*`. Mønstrene gjør det ikke. |
| Styling | Plain CSS per komponent (42 filer). Tokens i `primitive.css` og `semantic.css`. Nesten ingen hardkodede farger. Mange komponenter bruker primitive tokens (`--hvl-*`, `--space-*`) direkte (`QuizView.css` 34×, `SideMenuLayout.css` 26×). | Godt grunnlag. Mye gjentatt CSS (se 3). |
| i18n | Ingen. All tekst er hardkodet på engelsk, `lang="en"`, datoer i `en-GB`. | Se åpne spørsmål. |
| Tilgang | `RequireAccess` på `/manage-courses`. Lærersider i `course-view` er bare skjult i menyen (`courseMenu.ts`, `teacherOnly`), og hver side sjekker selv (`ContentSubmissionsPage:43`, `CourseSubmissionsPage:44`, `SubmissionGradingPage:43`). Backend sjekker alt. | Inkonsekvent, men ikke et sikkerhetshull. |
| Kodestil | 38 filer bruker doble anførselstegn og semikolon (mest data-features og `apiClient.ts`), resten enkle uten semikolon. Ingen formatter. | Bør automatiseres. |

### 2.5 Hovedproblemer (med eksempler)

**a) To akser i samme mappe.** Brukergrensesnittet for oppgaver og innleveringer er spredt:
- data: `features/assignments/`
- visning: `features/course-view/pages/ContentView/SubmittableContent.tsx`
- retting: `features/course-view/pages/SubmissionGradingPage/`
- ren innleveringslogikk: `features/course-view/submissionStats.ts`

Quiz er spredt på samme måte:
- data og regler: `features/quizzes/`
- visning: `course-view/pages/ContentView/QuizView.tsx`
- redigering: `course-management/module/ContentForm/QuizQuestionsEditor.tsx`

Navnet «assignments» sier altså ikke hvor oppgavesidene ligger. Det er greit når regelen er tydelig («domene = data og regler, feature = skjermbilder»), men i dag står `assignments/` og `course-view/` som likeverdige søsken.

**b) `course-view` er en søppelskuff på sidenivå.**
- `pages/` har 18 mapper som blander studentsider, lærersider (`CourseSubmissionsPage`, `ContentSubmissionsPage`, `SubmissionGradingPage`) og to byggeklosser som ikke er ruter:
  - `pages/ContentView` importeres av `module/pages/ModuleContentPage.tsx:3`
  - `pages/ContentTypeTable` importeres av `module/pages/ModuleContentTypePage.tsx:4`

  Det bryter README-regelen om at «bare routes importerer sider».
- `SubmissionGradingPage.tsx:21` importerer `../ContentView/SubmittableContent.css` fra en annen sides mappe.
- Den generiske `components/DataTable` (brukt på 8 sider) ligger i en feature i stedet for i `shared/ui`.

**c) Navnekollisjoner og navn som er vanskelige å gjette.**
- `CourseContentPage` finnes både i `course-view/pages/` (ett innholdselement) og i `course-management/pages/` (modullisten). `routes.tsx:18` må gi den nytt navn ved import (`CourseSectionContentPage`).
- `course-management/module/VisibleFromField.tsx` er en komponent uten egen mappe.
- `ContentForm/` ligger direkte under `module/`, ikke i `components/`.
- `course-management` mangler `components/`.

**d) Løse filer på feature-roten som hører til et domene.**
- `course-management/roleLabels.ts` har to varianter til: `course-view/pages/CoursePeoplePage/CoursePeoplePage.tsx:11` og `app/layout/CoursesPanel/CoursesPanel.tsx:14-15`.
- `course-view/submissionStats.ts` er ren logikk over `assignments`-typer.
- `course-management/fileTypes.ts` er en domeneregel om innhold, men brukes bare av `ContentForm` og kan derfor bli der den er.

**e) Dype relative imports.**
- 295 imports med tre eller flere `../`: 136 på tre nivåer, 135 på fire og 24 på fem.
- De dypeste er i `course-management/module/pages/*`. Eksempel: `ManageModulePage.tsx:3-16` har 14 slike linjer.
- README-eksempelet selv er `'../../../deadlines/components/DeadlineList'` (`README.md:90`).

**f) Store filer.**

| Fil | Linjer | Gjør |
|---|---|---|
| `course-view/pages/ContentView/QuizView.tsx` | 474 | Ta quiz, gjennomgang av forsøk, bestått-status og gjenværende forsøk, egne kort- og fakta-paneler |
| `course-view/pages/ContentView/SubmittableContent.tsx` | 386 | Vise oppgave eller kodeoppgave, innleveringsskjema (fil, tekst, gruppe), historikk, feilmeldinger. Definerer `Card`, `Layout` og `DeadlineFact` lokalt |
| `course-view/pages/SubmissionGradingPage/SubmissionGradingPage.tsx` | 366 | Én innlevering, karakterskjema, publisering, historikk. Kopierer kort- og panel-markup |
| `course-management/module/pages/ManageModulePage/ManageModulePage.tsx` | 347 | Modulens innholdstabell (håndskrevet `<table>`, `:250-313`), kladd (påkrevd, slett), tilgjengelighet, «+ Innhold»-knapper |
| `course-view/pages/CourseGroupsPage/CourseGroupsPage.tsx` | 341 | Grupper, bli med, forlat, bytt, slett, sammenleggbare bokser. Fire feilmeldingsfunksjoner (`:304-341`) |
| `course-management/module/ContentForm/ContentForm.tsx` | 265 | Opprett og rediger skjema for alle fire innholdstyper |
| `course-view/pages/ContentTypeTable/ContentTypeTable.tsx` | 209 | Tabell per innholdstype med frist- og quizstatus-kolonner |
| `course-view/pages/ContentSubmissionsPage/ContentSubmissionsPage.tsx` | 202 | Innleveringer for ett element, karakterpublisering |
| `course-management/pages/CourseContentPage/CourseContentPage.tsx` | 192 | Moduler med dra-og-slipp-rekkefølge og SaveBar |
| `course-management/pages/CourseMembersPage/CourseMembersPage.tsx` | 184 | Medlemmer med kladd for roller og fjerning |

Filene er store fordi de inneholder lokale underkomponenter og feilmeldingsfunksjoner som finnes i flere varianter. Det er duplisering, ikke god oppdeling. Å trekke ut det som står i seksjon 3 krymper dem uten at de må deles kunstig.

**g) Død kode.**
- `shared/ui/Pager/` brukes ikke av noen.
- Rundt 20 eksporter brukes bare internt i sin egen fil. Eksempler: `content/types.ts:150-187` (`Create*Input`), `content/contentTypes.ts:23` `contentTypeInfo`, `course-view/courseMenu.ts:5` `COURSE_MENU_ITEMS`, `shared/lib/dateFormat.ts:6` `SOON_MS`, `shared/paths.ts:14` `ModuleSection`.
- `modules/prerequisites.ts:50` `isAccessible` brukes bare av `mock/handlers.ts` og tester (det er OK hvis mocken trenger den).
- I `course-management/pages/CourseContentPage/CourseContentPage.css` er `.manage-module-row` definert to ganger (`:12-20` og `:54-57`), og `.manage-module-name` likeså.

**h) Utdatert dokumentasjon.**
- `frontend/README.md:52-56` sier at studentsidene er fjernet.
- Treet i README mangler `ConfirmDialog`, `SaveBar`, `RequiredMark` og `moveItem`.
- `shared/paths.ts:55` peker til `ui/manage/contentTypes.ts`, som ikke finnes.
- `RequireAccess.tsx:23` bruker stien `/courses/new` som eksempel.

**Avhengigheter (oppsummert).** Importmatrisen viser at `course-view` og `course-management` aldri importerer hverandre. De bruker bare data-features og `shared`. Kryss-feature-imports av komponenter:
- `dashboard` → `courses/components/CourseGrid`
- `dashboard` → `deadlines/components/DeadlineList`
- `course-management` → `courses/components/CourseForm`

Kobling gjennom queryKeys: `assignments/hooks.ts:3` → `grades/queryKeys`, og `content/hooks.ts:3` → `quizzes/queryKeys`. Den eneste syklusen på feature-nivå er `assignments` ↔ `grades`, der den ene retningen bare er type-import (`grades/types.ts:2`). Det er ufarlig.

## 3. Kandidater for gjenbruk (sortert etter gevinst)

Risiko: L = lav, M = middels, H = høy. «Linjer» er et grovt anslag på netto reduksjon.

| # | Hva som dupliseres (hvor) | Forslag (navn og API) | Plassering | Gevinst | Risiko | Verdt det? |
|---|---|---|---|---|---|---|
| 1 | Mutasjon + `onSuccess: Promise.all(invalidateQueries…)` som er byte-identisk bortsett fra `mutationFn`. `courses/hooks.ts`: `useEnrollInCourse` (`:53-66`), `useRemoveCourseEnrollment` (`:69-81`), `useUpdateCourseRole` (`:100-112`). `content/hooks.ts`: `useEditContent` og `useUpdateContent` (`:120-146`). `modules/hooks.ts`: `useCreateModule` og `useUpdateModule` (`:16-36`). `assignments/hooks.ts`: `useSaveGrade` og `useReleaseGrade` (`:128-143`). `groups/hooks.ts:35-41` har allerede `useGroupMutation` som mønster. | `useInvalidatingMutation<TVars, TData>(mutationFn, invalidates: (vars, data) => QueryKey[], options?)` | `shared/api/useInvalidatingMutation.ts` | 8 mutasjoner, ~60 linjer | L | **Ja.** Mønsteret er bevist i `groups`. |
| 2 | «Flat ut innholdet på tvers av moduler» (`modules.flatMap(m => content.byModule.get(String(m.id)) ?? [])`), 8 ganger: `CourseContentTypePage.tsx:20-23`, `CourseSubmissionsPage.tsx:34-37`, `CourseMenuLayout.tsx:36-54` (2×), `course-view/CourseContentPage.tsx:26-29`, `ContentSubmissionsPage.tsx:33`, `SubmissionGradingPage.tsx:33`, `CourseModulesPage.tsx:40`. «Finn element via contentId + `useContent`» er identisk i `ContentSubmissionsPage.tsx:32-35` og `SubmissionGradingPage.tsx:32-35`. | Ren funksjon `courseItems(content, modules, filter?) → { item, module }[]` + hook `useCourseItem(contentId) → { item, module, details }` | `courseItems` i `domain/content/courseItems.ts` (testbar). `useCourseItem` i `features/course-view/` (bruker `useCourseView`). | 8 + 3 steder, ~40 linjer | L | **Ja** |
| 3 | `ApiError` → brukermelding, 7 funksjoner i 6 filer: `submitError` (`SubmittableContent.tsx:278-287`), `gradeError` (`SubmissionGradingPage.tsx:167-174`), `editError` (`ContentDetailsPage.tsx:93-99`), `createError` (`CreateGroupForm.tsx:75-82`), `joinError`, `leaveError` og `deleteError` (`CourseGroupsPage.tsx:304-341`), samt inline i `ContentSubmissionsPage.tsx:67-70` | `errorMessage(error, byStatus: Partial<Record<number, string>>, fallback: string): string` | `shared/api/errorMessage.ts` | 9 steder, ~35 linjer. Konsekvente meldinger. | L | **Ja.** Lett å teste. |
| 4 | Kort- og panel-markup for innholdssider: `SubmittableContent.tsx` definerer `Layout` (`:59-66`), `Card` (`:69-76`) og `DeadlineFact` (`:232-252`), men `QuizView.tsx` (kort på `:69, 129, 376, 425, 439`, frist inline på `:158-166`) og `SubmissionGradingPage.tsx` (`:70, 220, 233, 269, 350`, panel `:68-157`) skriver det for hånd | Eksporter `ContentLayout({ main, aside })`, `Card({ id, title, children })` og `DeadlineFact({ deadline, now })`, og flytt CSS-en fra `SubmittableContent.css` | `features/course-view/content/parts/` | ~10 kort + 3 paneler, ~50 linjer. Fjerner også CSS-importen på tvers av sider. | L | **Ja.** Størst UI-gevinst. |
| 5 | `useQueries` + `combine` til `Map` etter id, 5 kopier: `useReviewSubmissionsFor` og `useGradeReviewsFor` (`assignments/hooks.ts`), `useContents` og `useCourseContent` (`content/hooks.ts`), `useMyQuizAttemptsFor` (`quizzes/hooks.ts`) | `useQueriesById<TId, T>(ids, queryKey: (id) => QueryKey, queryFn: (id) => Promise<T>, { keepNull? })` → `{ byId: Map<string, T>, isPending, isError }` | `shared/api/useQueriesById.ts` | 5 steder, ~40 linjer | M (`useGradeReviewsFor` skiller `null` fra `undefined`) | **Ja** |
| 6 | Tittelcelle (`TrailLink` + «Hidden from students» + `VisibleFromTag`) kopiert 3 ganger: `ContentTypeTable.tsx:73-86`, `CourseSubmissionsPage.tsx:70-78`, `ModuleOverviewPage.tsx:24-32`. «Hidden»-taggen finnes 5 ganger (også `ContentList.tsx:27` og `ContentView.tsx:35`). `Deadline`-cellen er ordrett lik i `ContentTypeTable.tsx:137-141` og `CourseSubmissionsPage.tsx:114-118`. | `<ContentTitle item to />` + kolonnefabrikker `titleColumn(toFn)`, `moduleColumn()`, `deadlineColumn(now)` | `features/course-view/components/contentColumns.tsx` | 3 + 2 + 5 steder, ~35 linjer | L | **Ja** |
| 7 | «Er oppgave eller kodeoppgave», 9 ganger: `ContentForm:153,246`, `ContentSubmissionsPage:50`, `ContentTypeTable:57,139,145`, `CourseMenuLayout:49`, `CourseSubmissionsPage:36,116`, `SubmissionGradingPage:52` | `isSubmittable(type: ContentType): boolean` | `domain/content/contentTypes.ts` | 9 steder. Én regel når en ny type kommer. | L | **Ja.** Billig. |
| 8 | Innlastingsblokken i layoutene er ordrett lik: `CourseLayout.tsx:14-31` og `ManageCourseLayout.tsx:15-27` (`useCourse` + `useCourseModules` + `useCourseRole`, de samme tre linjene for lasting og feil, den samme `sort` på `position`) | `useCourseWithModules(courseId) → { status: 'pending' \| 'error' \| 'ok', course, modules, role, errorText }`. Flytt sorteringen til `select` i `useCourseModules`. | `domain/courses/hooks.ts` (eller `domain/modules/hooks.ts` for sorteringen) | 2 steder, ~15 linjer | L | **Ja.** Ordrett kopi, og sorteringen hører hjemme i hooken. |
| 9 | CSS som gjentas: `font-family: var(--font-ui); font-size: var(--text-small)` 48×, fokusring 23× i 20 filer, listenullstilling 14×, store bokstaver + `letter-spacing: 0.04em` 11×, pille-tag 8 klasser (`.course-group-tag`, `.manage-change`, `.member-change`, `.manage-module-moved`, `.passed-badge`, `.submittable-mandatory`, `.teacher-tag`, `.quiz-question-result`), «dempet småtekst» med 4+ navn (`.data-table-muted`, `.submittable-muted`, `.content-list-meta`, `.content-view-hidden`), kant for ulagret endring (`box-shadow: inset …`) 6×, fieldset-nullstilling 4×, avkrysningsboksstørrelse 4× | `.text-meta`, `.eyebrow`, `.list-reset`, `.tag` (+ `.tag--danger`), `.is-unsaved` / `.is-pending-delete`, `.form-fieldset`. En global `:where(a, button, input, select, textarea, summary):focus-visible`-regel i `global.css`. | `shared/styles/utilities.css` (+ `global.css`, `forms.css`) | Flere hundre CSS-linjer | M (visuelle regresjoner) | **Ja, gradvis.** Én hjelpeklasse per PR. |
| 10 | Lasting og feil uten `QueryState`. Layouter med flere spørringer (`CourseLayout:27-29`, `ManageCourseLayout:20-22`), `useQueries`-resultater (`CourseContentTypePage:28-30`, `CourseSubmissionsPage:60-61`, `course-view/CourseContentPage:46-48`, `CourseModulesPage:37`) og rene enkeltspørringer (`CoursesPanel.tsx:69-71`, `ModuleLayout.tsx:131-133`, `SubmittableContent.tsx:133-134`, `QuizView.tsx:422-430`) | Utvid `QueryState` med `query` som også godtar `{ isPending, isError }` uten `data`, eller en liten `<LoadState pending error loading errorText>` for flere spørringer | `shared/ui/QueryState` | ~10 steder, konsekvente tekster og roller | L–M | **Ja**, men bare de rene enkeltspørringene først |
| 11 | Sammenleggbar boks (`toggle(Set)`, chevron, `aria-expanded`, `rotate(90deg)`, `prefers-reduced-motion`), 3 kopier: `CourseGroupsPage.tsx:48-57, 180-223` + CSS `:57-91`, `CourseModulesPage.tsx:22-31, 70-107` + CSS `:26-77`, `ModuleLayout.tsx:29, 72-100` + `SideMenuLayout.css:105-156` | `useToggleSet<T>() → { has, toggle }` + `.disclosure` / `.disclosure-chevron` i CSS. Ikke en komponent: markupen varierer for mye. | `shared/lib/useToggleSet.ts`, `shared/styles/utilities.css` | 3 steder, ~60 linjer (mest CSS) | L | **Ja** |
| 12 | Inline SVG: chevron `M9 6l6 6-6 6` 5× (`CoursesPanel.tsx:52,61`, `ModuleLayout.tsx:80`, `CourseGroupsPage.tsx:190`, `CourseModulesPage.tsx:83`), lukk-X 2× (`CoursesPanel.tsx:43`, `Topbar.tsx:37`), hake og kryss 2× (`PassedBadge.tsx:18-21`, `QuizView.tsx:357-360`) | `shared/ui/icons.tsx`: `ChevronIcon`, `CloseIcon`, `CheckIcon`, `CrossIcon`. `PassedBadge` får `labels?`-prop. | `shared/ui/icons.tsx` | 9 steder | L | **Ja.** Billig. |
| 13 | Quizstatus-logikk duplisert: beste forsøk (`QuizView.tsx:54-55`, `ContentTypeTable.tsx:162`), «N forsøk igjen / Ikke bestått / Bestått» (`QuizView.tsx:138-140, 189-203` og `ContentTypeTable.tsx:154-197`) | `bestAttempt(attempts)`, `quizStatus(quiz, attempts) → { kind: 'passed' \| 'notPassed' \| 'attemptsLeft', left }` | `domain/quizzes/passRule.ts` (som allerede har tester) | 2–3 steder. Viktigst for riktig oppførsel, ikke for antall linjer. | L | **Ja.** Domeneregel som skal være lik overalt. |
| 14 | Skjemaskjelett: `form-error role="alert"` 14×, `form-success role="status"` 6×, `isPending ? 'Xing…' : 'X'` 7×, «teacher-panel»-seksjon med `<h2>` og `<form>` identisk 3× (`AddModuleForm.tsx:27-43`, `AddMemberForm.tsx:41-105`, `CreateGroupForm.tsx:37-71`) | `<SubmitButton pending pendingLabel>`, `<FormStatus error success>`, `<Panel title>` | `shared/ui/Form/` | ~12 skjemaer, 2–4 linjer hver | L | **Delvis.** Mest for konsistens. Gjør det når filene endres uansett. |
| 15 | Små hjelpere: `formatDateTime(new Date(iso))` 28× (`SubmissionGradingPage` 8×), flertall `n === 1 ? …` ~18×, `${firstName} ${lastName}` 8× (`CourseGroupsPage.tsx:59-63` lager sin egen versjon av `usePeopleNames.nameOf`), `newestFirst` 3× (`assignments/hooks.ts:8-9`, `grades/hooks.ts:11-12`, `quizzes/hooks.ts:11-12`), «dette semesteret»-filter 2× (`CoursesPanel.tsx:31-35`, `StudentDashboard.tsx:15, 23-25`), rolleetiketter 3 varianter, sortering av medlemmer 2× (`CourseMembersPage.tsx:15-18`, `CoursePeoplePage.tsx:34-35`), `useState(() => new Date())` 7× | `formatDateTimeIso`, `plural(n, word)`, `fullName(user)`, `newestFirst` i `shared/lib`. `isThisTerm`, `ROLE_LABELS` og `byRoleThenName` i `domain/courses`. `useNow({ tickMs? })` i `shared/lib` (flytt fra `DeadlineList`). | se til venstre | ~70 steder totalt, små gevinster hver | L | **Ja**, samlet i 1–2 PR-er |
| 16 | `ManageModulePage` har en håndskrevet `<table>` (`:250-313`) med egen CSS (`ManageModulePage.css:43-110`) som gjenskaper `DataTable` | Gi `DataTable` `rowClassName?` og `Column.className?`, og bruk den her | `shared/ui/DataTable` | 1 sted, ~60 CSS-linjer | M (kladdemarkering per rad) | **Kanskje.** Bare når siden uansett skal endres. |
| 17 | Kladd + lagringsløkke + «Some changes could not be saved…» + bekreftelse for massesletting: `ManageModulePage.tsx:26-32, 53-84, 117-184` og `CourseMembersPage.tsx:20-22, 36-107` | `useDraftSave()` → `{ saving, error, saved, run }` | `shared/ui/SaveBar/` | 2 steder, ~30 linjer | M–H (kritisk lagringsflyt, ingen tester) | **Ikke nå.** Bare to forekomster. Vent på en tredje og på tester. |

**Bevisst ikke foreslått** (forekommer én–to ganger, eller variantene er for ulike):
- en generell `<Field>`-komponent: ~25 felt, men med suffikser, select, textarea og egne hint
- å slå sammen `CourseContentTypePage` og `ModuleContentTypePage`: de er allerede tynne omslag rundt `ContentTypeTable`
- `ContentList`, `MandatoryFact`, `PublishStatus`, `GradePublishing`: brukes én gang
- en modalkomponent: `ConfirmDialog` + `useConfirm` brukes allerede konsekvent (5 kall)
- `apiDelete`

Tre ting finnes ikke, og det er bra:
- ingen `Number()`-parsing av URL-id-er (id-er sammenlignes som strenger)
- ingen `useEffect`-synkronisering av skjemaer (skjemaer nullstilles via `key`)
- ingen direkte `queryClient` i sider

## 4. Anbefalt målstruktur

### 4.1 Tre
```text
src/
├── app/                      Oppstart og «liming». Det eneste stedet som kjenner alle features.
│   ├── main.tsx, App.tsx, queryClient.ts
│   ├── routes.tsx            Rutetreet. Importerer bare features/*/pages (via index.ts)
│   ├── auth/                 AuthGate, RequireAccess, LandingPage, LoginButton
│   ├── layout/               AppLayout, Topbar, Sidebar, CoursesPanel
│   └── ErrorBoundary/        (ny) fanger renderfeil rundt <Outlet/>
│
├── shared/                   Vet ingenting om emner, moduler eller innhold
│   ├── api/                  apiClient, errorMessage, useInvalidatingMutation, useQueriesById
│   ├── auth/                 auth, useAuth, access
│   ├── lib/                  dateFormat, safeUrl, useDebounced, moveItem, useNow, useToggleSet, plural, newestFirst
│   ├── navigation/           Trail
│   ├── styles/               primitive, semantic, global, forms, utilities.css (ny)
│   ├── ui/                   ActionMenu, ConfirmDialog, DataTable (flyttet), Form/ (ny), icons.tsx (ny),
│   │                         NotFoundPage, PageHeader, QueryState, RequiredMark, SaveBar, Tabs
│   ├── paths.ts              URL-er + rutemønstre (også de som useMatch bruker)
│   └── types.ts
│
├── domain/                   Datadomener: «hva er en X og hvordan henter/endrer vi den»
│   ├── <domene>/             assignments, content, courses, deadlines, grades, groups, modules, quizzes, users
│   │   ├── api.ts            API-kall (ingen JSX)
│   │   ├── hooks.ts          React Query; all invalidering her
│   │   ├── queryKeys.ts
│   │   ├── types.ts
│   │   ├── *.ts              rene regler og formatering (passRule, visibility, prerequisites,
│   │   │                     courseFormat, scoreFormat, contentTypes, submissionStats, …)
│   │   ├── components/       små presentasjonskomponenter for domenet, bare props
│   │   │                     (courses: CourseCard, CourseGrid, CourseForm; deadlines: DeadlineList)
│   │   └── README.md         API-kontrakten (som i dag)
│
└── features/                 Skjermbilder. Én mappe per del av appen brukeren navigerer i.
    ├── dashboard/            pages/DashboardPage (+ Student- og CourseManagerDashboard)
    ├── my-courses/           pages/CoursesPage (/courses)
    ├── profile/              pages/ProfilePage
    ├── course-view/          /courses/:courseId for medlemmer
    │   ├── courseContext.ts, courseMenu.ts, contentSections.ts, useCourseItem.ts, usePeopleNames.ts
    │   ├── components/       ContentList, MandatoryLabel, PassedBadge, VisibleFromTag, SideMenuLayout,
    │   │                     CourseMenuHeading, SubmissionAnswer, contentColumns.tsx
    │   ├── content/          ContentView, QuizView, SubmittableContent, ContentTypeTable, parts/ (Card, Layout, DeadlineFact).
    │   │                     Ikke-rutede byggeklosser som både emne- og modulsider bruker.
    │   ├── pages/            CourseLayout, CourseMenuLayout, CourseHome, CourseModules, CourseContentType,
    │   │                     CourseSectionItemPage (omdøpt fra CourseContentPage), People, Groups, Grades, Files
    │   ├── submissions/pages/  lærersider: CourseSubmissions, ContentSubmissions (+ GradePublishing), SubmissionGrading
    │   └── module/           moduleContext.ts + pages/ (ModuleLayout, Overview, ContentType, Content)
    └── course-management/    /manage-courses
        ├── manageCourseContext.ts, useManageBreadcrumbs.ts
        ├── components/       (ny) VisibleFromField
        ├── pages/            ManageCourses, NewCourse, ManageCourseLayout, CourseTabsLayout,
        │                     ModuleListPage (omdøpt fra CourseContentPage), CourseMembers, CourseDetails
        └── module/
            ├── manageModuleContext.ts, useManagedContent.ts
            ├── components/ContentForm/   (flyttet) ContentForm, QuizQuestionsEditor, quizQuestions, fileTypes
            └── pages/        ManageModuleLayout, ManageModulePage, NewContentPage, ContentDetailsPage
```

### 4.2 Begrunnelse og alternativer
- **Valgt: feature-basert med et eksplisitt datalag (`domain/`) og et delt lag (`shared/`).** Det er dagens README-modell, bare med de to aksene skilt i hver sin mappe. Endringen er nesten bare `git mv`, og den gir et svar på «hvor hører dette hjemme?» som kan læres på ett minutt:
  - Henter, endrer eller tolker det data om et domene? Da hører det til `domain/`.
  - Er det et skjermbilde eller en del av ett? Da hører det til `features/`.
  - Vet det ingenting om LMS-et? Da hører det til `shared/`.
- **Vurdert: domenene eier også sitt eget UI** (f.eks. at `assignments/` får innleveringssiden og retting). Valgt bort fordi sidene henger tett sammen med layout-contextene (`useCourseView`, `useModuleView`). Da måtte contexts deles på tvers av features, eller sider spres over mange mapper som alle avhenger av `course-view`. Det gir mer kobling, ikke mindre.
- **Vurdert: full Feature-Sliced Design** (`entities/`, `features/`, `widgets/`, `pages/`). Valgt bort fordi seks lag er for mye for ~13k linjer og en studentgruppe. `domain/` tilsvarer omtrent `entities` i FSD, uten de andre lagene.
- **Vurdert: beholde alt og bare innføre aliaser.** Det gir mye av gevinsten (seksjon 5, trinn 2), men løser ikke at `assignments/` og `course-view/` ser likeverdige ut. Hvis teamet ikke vil flytte mapper, er aliaser + oppryddingen i `course-view` + gjenbruk et godt minimum.

### 4.3 Regler
1. **Importretning:** `app → features → domain → shared`. Aldri oppover.
   - `features/X` importerer aldri `features/Y`. Det som trengs to steder, flyttes ned til `domain/` eller `shared/`.
   - `domain/X` kan importere `domain/Y` for typer og queryKeys (slik `assignments` → `grades` gjør i dag), men ikke komponenter.
2. **Domeneflater:** andre bruker et domene via `api.ts`, `hooks.ts`, `types.ts`, `queryKeys.ts`, de rene regelfilene og `components/X`. Vi innfører ikke en `index.ts` per domene: mappene er små, og en barrel per domene gjør tree-shaking og sirkelfeil vanskeligere å se. Komponentmappene beholder sin `index.ts`.
3. **Sider:** bare `app/routes.tsx` importerer `features/*/pages/*`. Byggeklosser som ikke er ruter ligger aldri i `pages/` (de legges i `components/` eller en navngitt undermappe som `content/`).
4. **Komponenter i `shared/ui` og `domain/*/components`** bruker bare props: ingen data-hooks, contexts eller `useParams` (som i dag, `README.md:67-70`).
5. **Når noe skal flyttes ned:** når det trengs på et tredje sted, eller allerede finnes i flere varianter (som rolleetikettene).
6. **Navngiving:**
   - Komponenter skrives i PascalCase, i mappe + `index.ts`. Hooks heter `useX.ts`, rene moduler camelCase `.ts`.
   - Sider heter `<Navn>Page`, layouter `<Navn>Layout`.
   - Ingen like sidenavn i to features.
   - Én kodestil, håndhevet av en formatter (se åpne spørsmål).
7. **Tekster for lasting, feil og tomme tilstander** går via `QueryState`. Feilmeldinger fra `ApiError` går via `errorMessage`.

### 4.4 Mappingtabell (det viktigste)
| I dag | Ny plassering |
|---|---|
| `features/{assignments,content,grades,groups,modules,quizzes,users}/` | `domain/<samme navn>/` |
| `features/courses/{api,hooks,queryKeys,types,courseFormat}.ts`, `components/*` | `domain/courses/…` |
| `features/courses/pages/CoursesPage` | `features/my-courses/pages/CoursesPage` |
| `features/deadlines/` (data + `DeadlineList`) | `domain/deadlines/` |
| `features/course-view/components/DataTable` | `shared/ui/DataTable` |
| `features/course-view/pages/{ContentView,ContentTypeTable}` | `features/course-view/content/` |
| `features/course-view/pages/{CourseSubmissionsPage,ContentSubmissionsPage,SubmissionGradingPage}` | `features/course-view/submissions/pages/` |
| `features/course-view/pages/CourseContentPage` | `features/course-view/pages/CourseSectionItemPage` |
| `features/course-view/submissionStats.ts` | `domain/assignments/submissionStats.ts` |
| `features/course-management/roleLabels.ts` (+ varianter i `CoursePeoplePage.tsx:11` og `CoursesPanel.tsx:14-15`) | `domain/courses/roleLabels.ts` |
| `features/course-management/pages/CourseContentPage` | `features/course-management/pages/ModuleListPage` |
| `features/course-management/module/ContentForm/` + `fileTypes.ts` | `features/course-management/module/components/ContentForm/` |
| `features/course-management/module/VisibleFromField.tsx` | `features/course-management/components/VisibleFromField/` |
| `shared/ui/Pager/` | slettes (ubrukt) |
| `useMatch('/courses/:courseId/…')` i 4 filer | mønstre i `shared/paths.ts` |

### 4.5 Automatisk håndheving

**Aliaser:** bruk `imports` i `package.json` (Node subpath imports). Det er én definisjon som Vite, TypeScript (`moduleResolution: bundler`) og Node (testene) alle forstår:
```jsonc
// frontend/package.json
"imports": {
  "#app/*": "./src/app/*",
  "#shared/*": "./src/shared/*",
  "#domain/*": "./src/domain/*",
  "#features/*": "./src/features/*"
}
```
Forbehold som må sjekkes i en liten spike før alt konverteres:
1. Løser Vite `#shared/ui/QueryState` (en mappe) til `index.ts`? Hvis ikke, kan vi i stedet bruke `paths` i `tsconfig.app.json` + `resolve.alias` i `vite.config.ts` med `@shared/…`.
2. Node krever filendelse. Modulene testene laster, har i dag bare `import type` (som fjernes) bortsett fra `shared/api/apiClient.ts:3` → `../auth/auth.ts`. Testede moduler må fortsatt importere verdier med `.ts`-endelse.

**Importgrenser med oxlint:** `import`-plugin + `no-restricted-imports` per mappe. Syntaksen for `overrides` og `patterns` må sjekkes mot den oxlint-versjonen dere har (1.88).
```jsonc
// frontend/.oxlintrc.json
{
  "plugins": ["react", "typescript", "oxc", "import"],
  "rules": {
    "react/rules-of-hooks": "error",
    "react/only-export-components": ["warn", { "allowConstantExport": true }],
    "import/no-cycle": "error"
  },
  "overrides": [
    { "files": ["src/shared/**"], "rules": { "no-restricted-imports": ["error", { "patterns": [
      { "group": ["#app/*", "#domain/*", "#features/*", "**/app/**", "**/domain/**", "**/features/**"],
        "message": "shared/ kan ikke kjenne app, domener eller features." } ] }] } },
    { "files": ["src/domain/**"], "rules": { "no-restricted-imports": ["error", { "patterns": [
      { "group": ["#app/*", "#features/*", "**/app/**", "**/features/**"],
        "message": "domain/ kan ikke importere skjermbilder." } ] }] } },
    { "files": ["src/features/**"], "rules": { "no-restricted-imports": ["error", { "patterns": [
      { "group": ["#app/*", "**/app/**"], "message": "features importerer ikke app/." },
      { "group": ["#features/*"], "message": "features importerer ikke hverandre; flytt det ned til domain/ eller shared/." } ] }] } }
  ]
}
```
Når alle imports på tvers av mapper bruker aliaser, kan du i tillegg forby `../../../*` med et mønster. Da blir aliaser den eneste måten å krysse lag på.

## 5. Migreringsplan

Hvert trinn er én PR som kan merges for seg, og appen kjører etter hvert trinn. Før hver merge: `npm run build` (som også typesjekker `mock/`), `npm run lint`, `npm test` og røyktesten under.

**Røyktest (`npm run dev:mock`).** Lag den som en sjekkliste, f.eks. i `frontend/mock/README.md`, før trinn 2:
- som student: dashboard, emne, modul, quiz (ta og se gjennomgang), oppgave (lever)
- som lærer: innleveringer, retting, publisering
- som kursansvarlig: administrer emne, modul (kladd, påkrevd, slett, lagre), medlemmer (rolle, fjern, lagre), nytt innhold, rediger innhold

| # | Steg | Gevinst | Innsats og risiko |
|---|---|---|---|
| 0 | **Sikkerhetsnett.** Røyktest-sjekkliste. Slå på `import/no-cycle` i oxlint. Legg til en formatter (oxfmt eller Prettier) i en egen PR med bare formatering. | Trygg grunn for resten | L |
| 1 | **Rydding uten endret oppførsel.** Slett `shared/ui/Pager`. Fjern `export` fra ubrukte eksporter (seksjon 2.5 g). Fjern de dupliserte selektorene i `CourseContentPage.css`. Rett utdaterte kommentarer (`paths.ts:55`, `RequireAccess.tsx:23`). Oppdater `frontend/README.md`. | Mindre støy | L |
| 2 | **Aliaser.** Spike (4.5), deretter en kodemod som skriver om alle imports på tvers av toppmapper til `#shared/…`, `#features/…` osv. Én PR, helst når få grener er åpne. | Fjerner 295 dype imports. Gjør alle senere flyttinger til enkle søk-og-erstatt. | L–M (merge-konflikter, ikke logikk) |
| 3 | **Grenseregler i oxlint** (4.5) for dagens struktur (`shared` ↛ `features`/`app`). | Reglene håndheves | L |
| 4 | **`course-view` innvendig.** `DataTable` → `shared/ui`. `ContentView` og `ContentTypeTable` → `content/`. Lærersider → `submissions/pages/`. Omdøp de to `CourseContentPage`. Flytt `roleLabels` ned og slå sammen de tre variantene. | Den største feature-en blir lesbar | L |
| 5 | **Rene hjelpere med tester:** `isSubmittable`, `courseItems`, `errorMessage`, `bestAttempt`/`quizStatus`, `newestFirst`, `plural`, `fullName`, `isThisTerm`, `formatDateTimeIso`. Én PR per gruppe. Hver får en `node:test` slik som i dag. | Kandidat 2, 3, 7, 13, 15 | L |
| 6 | **Hooks:** `useInvalidatingMutation` (domene for domene), `useQueriesById`, `useCourseWithModules` (+ sortering i `select`), `useCourseItem`, `useNow`, `useToggleSet`. | Kandidat 1, 5, 8, 11 | L–M |
| 7 | **UI-deler:** `Card`/`Layout`/`DeadlineFact` i `QuizView` og `SubmissionGradingPage`, kolonnefabrikker og `ContentTitle`, `icons.tsx`, `QueryState` på de rene enkeltspørringene. | Kandidat 4, 6, 10, 12. Krymper de tre største filene. | M (store filer uten tester: røyktest grundig) |
| 8 | **`domain/` og `features/` skilles.** `git mv` av datamappene til `domain/`, `CoursesPage` → `features/my-courses`. Med aliaser er dette søk-og-erstatt på `#features/<domene>` → `#domain/<domene>`. Utvid oxlint-reglene til full 4.5. | Den tydelige hovedregelen | L–M (mange filer, triviell diff) |
| 9 | **CSS-hjelpeklasser,** én per PR (`.text-meta`, global fokus, `.list-reset`, `.tag`, `.eyebrow`, `.is-unsaved`, `.form-fieldset`). | Kandidat 9 | M (visuelt: sammenlign skjermbilder) |
| 10 | **Valgfritt senere:** `ErrorBoundary` i `AppLayout`. Rutevakt for lærersider i `course-view` (erstatter de tre sjekkene i sidene). Skjemahjelpere (14). `DataTable` i `ManageModulePage` (16). `useDraftSave` (17), etter tester. | Robusthet og konsistens | M |

**Rekkefølgen:** trinn 0–3 gir mest orden for minst risiko og gjør alt etterpå billigere. Trinn 8 (den store flyttingen) kommer bevisst sent: da er aliasene på plass, og diffen blir bare stier. Hvis teamet vil stoppe et sted, er trinn 0–7 et fullgodt resultat i seg selv.

**Der det mangler tester før refaktorering:**
- Kladd- og lagringsflyten i `ManageModulePage.tsx:117-184` og `CourseMembersPage.tsx:71-107` (sekvensiell `mutateAsync`-løkke og delvis feil). Rør den ikke uten tester (Vitest + React Testing Library med `QueryClient` og stubbet `api`).
- Quiz-flyten i `QuizView.tsx` (forsøk, bestått, gjenværende forsøk). Regelen bør først flyttes til `passRule.ts` (trinn 5) og testes der.
- Retting og publisering i `SubmissionGradingPage.tsx:243-261` og `GradePublishing.tsx`.
- Invalidering i `hooks.ts` før trinn 6. En enkel test per domene som sjekker at mutasjonen invaliderer de riktige nøklene, med en ekte `QueryClient` og `invalidateQueries` som spion.
- Det finnes ingen infrastruktur for komponenttester i dag (`package.json` har bare `node --test`). Å innføre Vitest er et eget valg (se 6).

## 6. Åpne spørsmål

1. **Hvor stor endring vil teamet ha?** Den fulle anbefalingen tar med flyttingen `domain/`/`features/` (trinn 8). Minimumsvarianten er trinn 0–7. Det krever enighet i gruppen, siden alle åpne grener får konflikter.
2. **Navnet `domain/`:** alternativer er `data/`, `entities/` (FSD) eller `api/`. `domain/` er valgt fordi mappene også inneholder regler og små komponenter, ikke bare datahenting.
3. **Hører lærersidene for innleveringer og retting til i `course-view`?** README (`README.md:49`) sier at «grading belongs elsewhere», men de ligger under `/courses/:id/submissions` og bruker `useCourseView`. Jeg foreslår en undermappe i stedet for en egen feature. Er det tenkt at de skal få en egen URL eller layout senere?
4. **Komponenttester:** vil dere innføre Vitest + React Testing Library, eller holde dere til rene `node:test`-tester og manuell røyktest? Det påvirker hvor trygt trinn 6–7 og 10 kan gjøres.
5. **Formatter:** oxfmt passer med oxlint, Prettier er mer utbredt. I dag er stilen blandet (anførselstegn og semikolon).
6. **Aliassyntaks:** `#shared/…` (Node subpath imports, fungerer også i testene) eller `@shared/…` (tsconfig `paths` + Vite `alias`, mer vanlig, men testene må da bruke relative stier). Det avhenger av spiken i trinn 2.
7. **i18n:** all tekst er engelsk og hardkodet. Skal appen være norsk eller flerspråklig? I så fall bør tekster samles før det dupliseres mer (feilmeldingene i kandidat 3 er et naturlig startsted).
8. **To brødsmulesystemer** (Trail via history-state og URL-baserte i `ModuleLayout` og `useManageBreadcrumbs`): er det et bevisst valg at begge skal finnes? Jeg forstår ikke helt hvorfor Trail trengs når URL-ene er hierarkiske. Det er derfor ikke foreslått endret.
9. **`mock/handlers.ts` (1 095 linjer)** bruker typer og regler fra `src/features`. Etter trinn 8 peker den på `src/domain`. Skal mocken også få en grenseregel (bare `domain/` og `shared/`)?
