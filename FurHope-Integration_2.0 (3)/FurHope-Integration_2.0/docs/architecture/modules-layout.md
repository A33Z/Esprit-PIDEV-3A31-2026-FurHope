# FurHope Module Layout

The codebase is organized by module under `modules/`:

- `modules/social/`
  - `java/`: social feed, posts, comments, reactions (`com.esprit.furhope`)
  - `resources/`: social FXML + social CSS (`fxml/`, `css/`)
- `modules/products/`
  - `java/`: shop, cart, product + payment (`com.projet`)
  - `resources/`: shop/product FXML + product images
- `modules/user/`
  - `java/`: auth/profile/admin/reclamation + auth integrations
  - `resources/`: sign-in/sign-up/welcome/profile/admin screens + `images/`, `opencv/`
- `modules/hotel/`
  - `java/`: hotel discovery/management/reservations + hotel integrations
  - `resources/`: hotel dashboard/details/map/support screens
- `modules/adoption/`
  - `java/`: adoption/animal module (`com.esprit.animal`)
  - `resources/`: adoption FXML/i18n/assets (`animal/`)
- `modules/vetcare/`
  - `java/`: vet availability/rendezvous controllers + models/services
  - `resources/`: vet care FXML screens
- `modules/common/`
  - `java/`: shared app bootstrap/config/utils
  - `resources/`: shared root styles/config/sql
- `modules/social/`
  - social feed runtime is JDBC-only (no separate social Spring backend)

## Build configuration

`pom.xml` uses `build-helper-maven-plugin` to add all module `java/` and `resources/` directories to the Maven build, so runtime paths (like `"/signin.fxml"` and `"/css/app.css"`) remain unchanged.

## Non-code structure

- `docs/`: project documentation (`readme/`, `architecture/`)
- `infrastructure/database/`: SQL/bootstrap scripts
- `infrastructure/secrets/`: local and example secret property files
