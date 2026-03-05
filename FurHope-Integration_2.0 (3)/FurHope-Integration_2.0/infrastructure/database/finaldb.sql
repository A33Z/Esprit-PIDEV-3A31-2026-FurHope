-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Mar 03, 2026 at 11:57 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `hamza`
--

-- --------------------------------------------------------

--
-- Table structure for table `adoption_request`
--

CREATE TABLE `adoption_request` (
  `id` int(11) NOT NULL,
  `request_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` enum('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  `animal_id` int(11) NOT NULL,
  `client_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `animal`
--

CREATE TABLE `animal` (
  `idAnimal` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `species` varchar(50) NOT NULL,
  `breed` varchar(100) DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  `gender` enum('MALE','FEMALE') DEFAULT NULL,
  `description` text DEFAULT NULL,
  `status` enum('AVAILABLE','ADOPTED','UNAVAILABLE') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Stand-in structure for view `api_comment`
-- (See below for the actual view)
--
CREATE TABLE `api_comment` (
`id` bigint(20)
,`post_id` bigint(20)
,`author_id` int(11)
,`parent_comment_id` bigint(20)
,`body` text
,`status` enum('ACTIVE','HIDDEN','DELETED')
,`created_at` timestamp
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `api_post`
-- (See below for the actual view)
--
CREATE TABLE `api_post` (
`id` bigint(20)
,`author_id` int(11)
,`caption` text
,`media_type` enum('NONE','IMAGE','VIDEO')
,`media_path` varchar(500)
,`thumbnail_path` varchar(500)
,`duration_seconds` int(11)
,`likes_count` int(11)
,`dislikes_count` int(11)
,`shares_count` int(11)
,`comments_count` int(11)
,`visibility` enum('PUBLIC','FRIENDS','PRIVATE')
,`status` enum('ACTIVE','HIDDEN','DELETED')
,`created_at` timestamp
,`updated_at` timestamp
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `api_user`
-- (See below for the actual view)
--
CREATE TABLE `api_user` (
`id_user` int(11)
,`name` varchar(201)
,`email` varchar(150)
,`phone` varchar(20)
,`password_hash` binary(0)
,`password` varchar(255)
,`role` varchar(20)
,`active` tinyint(1)
,`created_at` timestamp
);

-- --------------------------------------------------------

--
-- Table structure for table `client_gestion`
--

CREATE TABLE `client_gestion` (
  `id` int(11) NOT NULL,
  `full_name` varchar(120) NOT NULL,
  `email` varchar(150) NOT NULL,
  `phone` varchar(40) DEFAULT NULL,
  `city` varchar(80) DEFAULT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `comment`
--

CREATE TABLE `comment` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `author_id` int(11) NOT NULL,
  `parent_comment_id` bigint(20) DEFAULT NULL,
  `body` text NOT NULL,
  `status` enum('ACTIVE','HIDDEN','DELETED') NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `comment`
--

INSERT INTO `comment` (`id`, `post_id`, `author_id`, `parent_comment_id`, `body`, `status`, `created_at`) VALUES
(1, 1, 5, NULL, 'helo', 'ACTIVE', '2026-03-03 22:16:02');

-- --------------------------------------------------------

--
-- Table structure for table `compte`
--

CREATE TABLE `compte` (
  `id_compte` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `username` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('CLIENT','ADMIN','MANAGER','VET','HOTEL') NOT NULL,
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `disponibilite`
--

CREATE TABLE `disponibilite` (
  `id_disponibilite` int(11) NOT NULL,
  `vet_id` int(11) NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `is_available` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `friendship`
--

CREATE TABLE `friendship` (
  `id` bigint(20) NOT NULL,
  `user1_id` int(11) NOT NULL,
  `user2_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `friendship`
--

INSERT INTO `friendship` (`id`, `user1_id`, `user2_id`, `created_at`) VALUES
(1, 2, 3, '2026-03-01 21:06:54'),
(2, 1, 2, '2026-03-02 05:03:54'),
(3, 1, 5, '2026-03-03 21:21:23');

-- --------------------------------------------------------

--
-- Table structure for table `friend_request`
--

CREATE TABLE `friend_request` (
  `id` bigint(20) NOT NULL,
  `sender_id` int(11) NOT NULL,
  `receiver_id` int(11) NOT NULL,
  `status` enum('PENDING','ACCEPTED','DECLINED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `friend_request`
--

INSERT INTO `friend_request` (`id`, `sender_id`, `receiver_id`, `status`, `created_at`) VALUES
(1, 3, 2, 'ACCEPTED', '2026-03-01 21:06:19'),
(2, 1, 2, 'ACCEPTED', '2026-03-02 05:03:54'),
(3, 1, 5, 'DECLINED', '2026-03-03 21:20:03'),
(4, 5, 1, 'ACCEPTED', '2026-03-03 21:21:04');

-- --------------------------------------------------------

--
-- Table structure for table `hotel`
--

CREATE TABLE `hotel` (
  `id` int(11) NOT NULL,
  `name` varchar(100) NOT NULL,
  `address` varchar(255) NOT NULL,
  `manager_id` int(11) DEFAULT NULL,
  `capacity` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `hotel`
--

INSERT INTO `hotel` (`id`, `name`, `address`, `manager_id`, `capacity`, `created_at`) VALUES
(1, 'Conrad', '102 North End Avenue, New York', 1, 0, '2026-03-02 06:44:41'),
(2, 'The Frederick Hotel', 'New York', 1, 0, '2026-03-02 06:44:41'),
(3, 'Latham', '4 East 28th Street, New York', 1, 0, '2026-03-02 06:44:42'),
(4, 'Mandarin Oriental', '80 Columbus Circle, New York', 1, 0, '2026-03-02 06:44:42'),
(5, 'The Standard, East Village', '25 Cooper Square, New York', 1, 0, '2026-03-02 06:44:42'),
(6, 'Tempo by Hilton New York Times Square', '1568 Broadway, New York', 1, 0, '2026-03-02 06:44:43'),
(7, 'Clarion Hotel Park Avenue', 'New York', 1, 0, '2026-03-02 06:44:43'),
(8, 'Fitzpatrick Grand Central Hotel', '141 East 44th Street, New York', 1, 0, '2026-03-02 06:44:44');

-- --------------------------------------------------------

--
-- Table structure for table `manager_account`
--

CREATE TABLE `manager_account` (
  `id` bigint(20) NOT NULL,
  `manager_id` varchar(64) NOT NULL,
  `display_name` varchar(128) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `password_salt` varchar(255) NOT NULL,
  `password_iterations` int(11) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `notification`
--

CREATE TABLE `notification` (
  `id` bigint(20) NOT NULL,
  `recipient_id` int(11) NOT NULL,
  `actor_id` int(11) NOT NULL,
  `type` enum('POST_LIKE','POST_DISLIKE','POST_COMMENT','COMMENT_REPLY','COMMENT_LIKE') NOT NULL,
  `post_id` bigint(20) DEFAULT NULL,
  `comment_id` bigint(20) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `notification`
--

INSERT INTO `notification` (`id`, `recipient_id`, `actor_id`, `type`, `post_id`, `comment_id`, `message`, `is_read`, `created_at`) VALUES
(2, 2, 3, 'POST_LIKE', 18, NULL, 'Someone liked your post', 1, '2026-03-01 21:40:25'),
(3, 2, 3, 'POST_COMMENT', 18, 37, 'New comment on your post', 1, '2026-03-01 21:40:38'),
(9, 3, 2, 'POST_LIKE', 22, NULL, 'Someone liked your post', 1, '2026-03-02 03:46:32'),
(10, 3, 2, 'POST_COMMENT', 22, 52, 'New comment on your post', 1, '2026-03-02 03:46:38'),
(11, 3, 2, 'COMMENT_REPLY', 22, 52, 'replied to your comment', 1, '2026-03-02 03:46:38'),
(12, 3, 1, 'POST_COMMENT', 23, 54, 'New comment on your post', 0, '2026-03-02 05:03:28'),
(13, 3, 1, 'POST_COMMENT', 23, 55, 'New comment on your post', 0, '2026-03-02 05:04:21'),
(14, 3, 1, 'COMMENT_REPLY', 23, 55, 'replied to your comment', 0, '2026-03-02 05:04:21'),
(15, 4, 5, 'POST_COMMENT', 1, 1, 'New comment on your post', 0, '2026-03-03 22:16:02');

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `customer_email` varchar(255) NOT NULL,
  `total_amount` double NOT NULL,
  `transaction_id` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `orders`
--

INSERT INTO `orders` (`id`, `created_at`, `customer_email`, `total_amount`, `transaction_id`) VALUES
(1, '2026-03-03 23:28:27.776424', 'safwenyahyaoui047@gmail.com', 20, '498c0b6a-84b5-4b8f-80fd-8edf6566a49b');

-- --------------------------------------------------------

--
-- Table structure for table `panier`
--

CREATE TABLE `panier` (
  `id` int(11) NOT NULL,
  `idProduit` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `totalP` double NOT NULL,
  `totalt` double NOT NULL,
  `qty` int(11) NOT NULL,
  `client_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `panier`
--

INSERT INTO `panier` (`id`, `idProduit`, `title`, `totalP`, `totalt`, `qty`, `client_id`) VALUES
(5, 4, 'Comfort Carrier', 72, 6, 1, 1);

-- --------------------------------------------------------

--
-- Table structure for table `payments`
--

CREATE TABLE `payments` (
  `payment_type` varchar(31) NOT NULL,
  `id` bigint(20) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `confirmation_code` varchar(255) DEFAULT NULL,
  `confirmation_expires_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `customer_email` varchar(255) NOT NULL,
  `customer_name` varchar(255) NOT NULL,
  `status` varchar(255) NOT NULL,
  `transaction_id` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `payments`
--

INSERT INTO `payments` (`payment_type`, `id`, `amount`, `confirmation_code`, `confirmation_expires_at`, `created_at`, `customer_email`, `customer_name`, `status`, `transaction_id`) VALUES
('CARD', 1, 20.00, NULL, NULL, '2026-03-03 22:28:06.260947', 'safwenyahyaoui047@gmail.com', 'E2E User', 'SUCCESS', '498c0b6a-84b5-4b8f-80fd-8edf6566a49b'),
('CARD', 2, 20.00, '302220', '2026-03-03 23:00:50.945252', '2026-03-03 22:50:50.945252', 'smoke@example.com', 'Smoke User', 'PENDING', 'd6eec768-b749-4483-a66d-55f303c4065d'),
('PAYPAL', 3, 64.80, '619046', '2026-03-03 23:05:05.330381', '2026-03-03 22:55:05.330381', 'safwenyahyaoui047@gmail.com', 'safwen', 'PENDING', 'ef7cbf5d-8a09-4443-9d32-00d953529f2d');

-- --------------------------------------------------------

--
-- Table structure for table `post`
--

CREATE TABLE `post` (
  `id` bigint(20) NOT NULL,
  `author_id` int(11) NOT NULL,
  `caption` text DEFAULT NULL,
  `media_type` enum('NONE','IMAGE','VIDEO') NOT NULL DEFAULT 'NONE',
  `media_path` varchar(500) DEFAULT NULL,
  `thumbnail_path` varchar(500) DEFAULT NULL,
  `duration_seconds` int(11) DEFAULT NULL,
  `likes_count` int(11) NOT NULL DEFAULT 0,
  `dislikes_count` int(11) NOT NULL DEFAULT 0,
  `shares_count` int(11) NOT NULL DEFAULT 0,
  `comments_count` int(11) NOT NULL DEFAULT 0,
  `visibility` enum('PUBLIC','FRIENDS','PRIVATE') NOT NULL DEFAULT 'PUBLIC',
  `status` enum('ACTIVE','HIDDEN','DELETED') NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `post`
--

INSERT INTO `post` (`id`, `author_id`, `caption`, `media_type`, `media_path`, `thumbnail_path`, `duration_seconds`, `likes_count`, `dislikes_count`, `shares_count`, `comments_count`, `visibility`, `status`, `created_at`, `updated_at`) VALUES
(1, 4, 'test integration', 'IMAGE', 'C:\\Users\\safwe\\Downloads\\myimg.jpeg', NULL, NULL, 1, 0, 0, 1, 'PUBLIC', 'ACTIVE', '2026-03-02 07:42:21', '2026-03-03 22:16:02');

-- --------------------------------------------------------

--
-- Table structure for table `post_reaction`
--

CREATE TABLE `post_reaction` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `reaction` enum('LIKE','DISLIKE') NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `post_reaction`
--

INSERT INTO `post_reaction` (`id`, `post_id`, `user_id`, `reaction`, `created_at`) VALUES
(2, 17, 1, 'DISLIKE', '2026-02-28 22:20:00'),
(4, 17, 2, 'DISLIKE', '2026-03-01 19:48:46'),
(5, 21, 2, 'LIKE', '2026-03-01 21:32:55'),
(6, 18, 3, 'LIKE', '2026-03-01 21:40:25'),
(9, 22, 3, 'LIKE', '2026-03-02 03:44:58'),
(10, 22, 2, 'LIKE', '2026-03-02 03:46:32'),
(11, 23, 3, 'LIKE', '2026-03-02 04:20:43'),
(13, 23, 1, 'LIKE', '2026-03-02 05:03:28'),
(14, 1, 4, 'LIKE', '2026-03-02 07:42:47');

-- --------------------------------------------------------

--
-- Table structure for table `post_report`
--

CREATE TABLE `post_report` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `reporter_user_id` bigint(20) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `post_report`
--

INSERT INTO `post_report` (`id`, `post_id`, `reporter_user_id`, `reason`, `created_at`) VALUES
(1, 17, 1, 'bad', '2026-02-28 22:38:49'),
(2, 17, 2, 'bad', '2026-02-28 22:48:48'),
(3, 23, 1, 'integration-check', '2026-03-02 05:03:28');

-- --------------------------------------------------------

--
-- Table structure for table `post_share`
--

CREATE TABLE `post_share` (
  `id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `post_share`
--

INSERT INTO `post_share` (`id`, `post_id`, `user_id`, `created_at`) VALUES
(4, 23, 1, '2026-03-02 05:03:28');

-- --------------------------------------------------------

--
-- Table structure for table `produit`
--

CREATE TABLE `produit` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `price` double NOT NULL,
  `tva` double NOT NULL,
  `image` varchar(255) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `stock` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `produit`
--

INSERT INTO `produit` (`id`, `title`, `price`, `tva`, `image`, `description`, `stock`) VALUES
(4, 'Comfort Carrier', 72, 6, 'pet-carrier.png', 'Travel carrier with ventilation and reinforced frame.', 19),
(5, 'Medical Care Kit', 39.9, 4.5, 'pet-medical.png', 'First-aid essentials for minor pet care emergencies.', 30),
(6, 'Accessory Pack', 15, 1.5, 'pet-accessory.png', 'Leash, bowl, and grooming accessories in one pack.', 70),
(9, 'Pet Food Premium', 25, 3, 'pet-food.png', 'Healthy dry food with balanced vitamins for daily nutrition.', 60),
(10, 'Interactive Pet Toy', 18.5, 2, 'pet-toy.png', 'Soft, durable toy for playtime and mental stimulation.', 45);

-- --------------------------------------------------------

--
-- Table structure for table `promo_codes`
--

CREATE TABLE `promo_codes` (
  `id` bigint(20) NOT NULL,
  `code` varchar(255) NOT NULL,
  `discount_percent` double NOT NULL,
  `expiration_date` datetime(6) NOT NULL,
  `usage_limit` int(11) NOT NULL,
  `used_count` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `promo_codes`
--

INSERT INTO `promo_codes` (`id`, `code`, `discount_percent`, `expiration_date`, `usage_limit`, `used_count`) VALUES
(1, 'azerty', 10, '2026-04-02 23:27:14.907832', 100, 1);

-- --------------------------------------------------------

--
-- Table structure for table `reclamation`
--

CREATE TABLE `reclamation` (
  `id` int(11) NOT NULL,
  `client_id` int(11) NOT NULL,
  `sujet` varchar(180) NOT NULL,
  `description` text NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'OPEN',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `reclamation`
--

INSERT INTO `reclamation` (`id`, `client_id`, `sujet`, `description`, `status`, `created_at`) VALUES
(2, 2, 'test', 'i have problem', 'OPEN', '2026-02-15 08:55:49'),
(3, 1, 'problemtechnique', 'there is problems in your website pleasecheck', 'OPEN', '2026-02-15 11:59:29');

-- --------------------------------------------------------

--
-- Table structure for table `rendezvous`
--

CREATE TABLE `rendezvous` (
  `id_rdv` int(11) NOT NULL,
  `appointment_date` date NOT NULL,
  `appointment_time` time NOT NULL,
  `status` varchar(30) DEFAULT 'pending',
  `description` text DEFAULT NULL,
  `client_id` int(11) NOT NULL,
  `vet_id` int(11) NOT NULL,
  `animal_id` int(11) NOT NULL,
  `disponibilite_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `reponse`
--

CREATE TABLE `reponse` (
  `id` int(11) NOT NULL,
  `reclamation_id` int(11) NOT NULL,
  `admin_id` int(11) NOT NULL,
  `message` text NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `reponse`
--

INSERT INTO `reponse` (`id`, `reclamation_id`, `admin_id`, `message`, `created_at`) VALUES
(1, 2, 1, 'dear user we will look into the problems', '2026-02-15 08:57:09');

-- --------------------------------------------------------

--
-- Table structure for table `reservation`
--

CREATE TABLE `reservation` (
  `id` int(11) NOT NULL,
  `client_id` int(11) DEFAULT NULL,
  `animal_id` int(11) DEFAULT NULL,
  `hotel_id` int(11) DEFAULT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `reservation_date` date NOT NULL,
  `guest_count` int(11) NOT NULL DEFAULT 1,
  `nightly_rate` decimal(10,2) NOT NULL DEFAULT 85.00,
  `total_price` decimal(10,2) NOT NULL DEFAULT 85.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user`
--

CREATE TABLE `user` (
  `id` int(11) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `last_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `city` varchar(100) DEFAULT NULL,
  `profile_image_path` varchar(1024) DEFAULT NULL,
  `role` varchar(20) DEFAULT NULL,
  `active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `id_user` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user`
--

INSERT INTO `user` (`id`, `first_name`, `last_name`, `email`, `password`, `phone`, `address`, `city`, `role`, `active`, `created_at`, `id_user`) VALUES
(1, 'Hamza', 'Ben Yahia', 'hamza.benyahia@esprit.tn', '123456', '24558193', 'Rue de Paris', 'Ariana', 'ADMIN', 1, '2026-02-14 09:16:04', 1),
(2, 'hamza', 'benyahia', 'donniedarko@gmail.com', '123456', '53035155', '23ruedeliberte', 'megrine', 'VETERINAIRE', 1, '2026-02-14 11:14:14', 2),
(3, 'fzef', 'fzaazf', 'hjbhjazdf@gmail.com', '123456', '74852963', 'zarrouk', 'zak', 'VETERINAIRE', 1, '2026-02-14 16:42:14', 3),
(4, 'adem', 'ziri', 'ziriadem33@gmail.com', 'Ademadouma12', '27938446', '23rueelbassa', 'haydrebek', 'VETERINAIRE', 1, '2026-02-15 11:09:08', 4),
(5, 'zakaria', 'zarouk', 'zarrouk.zakaria@esprit.tn', 'Zakaria12', '74859123', 'nahjelkouki', 'houkouma', 'VETERINAIRE', 1, '2026-02-15 11:57:54', 5);

-- --------------------------------------------------------

--
-- Structure for view `api_comment`
--
DROP TABLE IF EXISTS `api_comment`;

CREATE ALGORITHM=UNDEFINED DEFINER=`safwen`@`localhost` SQL SECURITY DEFINER VIEW `api_comment`  AS SELECT `c`.`id` AS `id`, `c`.`post_id` AS `post_id`, `c`.`author_id` AS `author_id`, `c`.`parent_comment_id` AS `parent_comment_id`, `c`.`body` AS `body`, `c`.`status` AS `status`, `c`.`created_at` AS `created_at` FROM `comment` AS `c` ;

-- --------------------------------------------------------

--
-- Structure for view `api_post`
--
DROP TABLE IF EXISTS `api_post`;

CREATE ALGORITHM=UNDEFINED DEFINER=`safwen`@`localhost` SQL SECURITY DEFINER VIEW `api_post`  AS SELECT `p`.`id` AS `id`, `p`.`author_id` AS `author_id`, `p`.`caption` AS `caption`, `p`.`media_type` AS `media_type`, `p`.`media_path` AS `media_path`, `p`.`thumbnail_path` AS `thumbnail_path`, `p`.`duration_seconds` AS `duration_seconds`, `p`.`likes_count` AS `likes_count`, `p`.`dislikes_count` AS `dislikes_count`, `p`.`shares_count` AS `shares_count`, `p`.`comments_count` AS `comments_count`, `p`.`visibility` AS `visibility`, `p`.`status` AS `status`, `p`.`created_at` AS `created_at`, `p`.`updated_at` AS `updated_at` FROM `post` AS `p` ;

-- --------------------------------------------------------

--
-- Structure for view `api_user`
--
DROP TABLE IF EXISTS `api_user`;

CREATE ALGORITHM=UNDEFINED DEFINER=`safwen`@`localhost` SQL SECURITY DEFINER VIEW `api_user`  AS SELECT `u`.`id` AS `id_user`, concat(`u`.`first_name`,' ',`u`.`last_name`) AS `name`, `u`.`email` AS `email`, `u`.`phone` AS `phone`, NULL AS `password_hash`, `u`.`password` AS `password`, `u`.`role` AS `role`, `u`.`active` AS `active`, `u`.`created_at` AS `created_at` FROM `user` AS `u` ;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `adoption_request`
--
ALTER TABLE `adoption_request`
  ADD PRIMARY KEY (`id`),
  ADD KEY `animal_id` (`animal_id`),
  ADD KEY `fk_adoption_client` (`client_id`);

--
-- Indexes for table `animal`
--
ALTER TABLE `animal`
  ADD PRIMARY KEY (`idAnimal`);

--
-- Indexes for table `client_gestion`
--
ALTER TABLE `client_gestion`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `comment`
--
ALTER TABLE `comment`
  ADD PRIMARY KEY (`id`),
  ADD KEY `author_id` (`author_id`),
  ADD KEY `parent_comment_id` (`parent_comment_id`),
  ADD KEY `idx_comment_post` (`post_id`);

--
-- Indexes for table `compte`
--
ALTER TABLE `compte`
  ADD PRIMARY KEY (`id_compte`),
  ADD UNIQUE KEY `username` (`username`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `disponibilite`
--
ALTER TABLE `disponibilite`
  ADD PRIMARY KEY (`id_disponibilite`),
  ADD KEY `vet_id` (`vet_id`);

--
-- Indexes for table `friendship`
--
ALTER TABLE `friendship`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_friendship` (`user1_id`,`user2_id`),
  ADD UNIQUE KEY `uq_friendship_pair` (`user1_id`,`user2_id`),
  ADD KEY `fk_fs_u2` (`user2_id`);

--
-- Indexes for table `friend_request`
--
ALTER TABLE `friend_request`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_request` (`sender_id`,`receiver_id`),
  ADD KEY `fk_fr_receiver` (`receiver_id`);

--
-- Indexes for table `hotel`
--
ALTER TABLE `hotel`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_hotel_manager` (`manager_id`);

--
-- Indexes for table `manager_account`
--
ALTER TABLE `manager_account`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `manager_id` (`manager_id`);

--
-- Indexes for table `notification`
--
ALTER TABLE `notification`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_recipient_read_time` (`recipient_id`,`is_read`,`created_at`),
  ADD KEY `fk_notif_actor` (`actor_id`),
  ADD KEY `fk_notif_post` (`post_id`),
  ADD KEY `fk_notif_comment` (`comment_id`);

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKxwcsepu5t1hoe04v5i8xhk5y` (`transaction_id`);

--
-- Indexes for table `panier`
--
ALTER TABLE `panier`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idProduit` (`idProduit`),
  ADD KEY `fk_panier_client` (`client_id`);

--
-- Indexes for table `payments`
--
ALTER TABLE `payments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKlryndveuwa4k5qthti0pkmtlx` (`transaction_id`);

--
-- Indexes for table `post`
--
ALTER TABLE `post`
  ADD PRIMARY KEY (`id`),
  ADD KEY `author_id` (`author_id`),
  ADD KEY `idx_post_feed` (`created_at`);

--
-- Indexes for table `post_reaction`
--
ALTER TABLE `post_reaction`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_post_user` (`post_id`,`user_id`);

--
-- Indexes for table `post_report`
--
ALTER TABLE `post_report`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_post_reporter` (`post_id`,`reporter_user_id`);

--
-- Indexes for table `post_share`
--
ALTER TABLE `post_share`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_post_user_share` (`post_id`,`user_id`);

--
-- Indexes for table `produit`
--
ALTER TABLE `produit`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `promo_codes`
--
ALTER TABLE `promo_codes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKj9mo0xgfs34t6e3c17anidd83` (`code`);

--
-- Indexes for table `reclamation`
--
ALTER TABLE `reclamation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_reclamation_client` (`client_id`);

--
-- Indexes for table `rendezvous`
--
ALTER TABLE `rendezvous`
  ADD PRIMARY KEY (`id_rdv`),
  ADD KEY `client_id` (`client_id`),
  ADD KEY `vet_id` (`vet_id`),
  ADD KEY `animal_id` (`animal_id`),
  ADD KEY `disponibilite_id` (`disponibilite_id`);

--
-- Indexes for table `reponse`
--
ALTER TABLE `reponse`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_reponse_reclamation` (`reclamation_id`),
  ADD KEY `fk_reponse_admin` (`admin_id`);

--
-- Indexes for table `reservation`
--
ALTER TABLE `reservation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_reservation_client` (`client_id`),
  ADD KEY `fk_reservation_animal` (`animal_id`),
  ADD KEY `fk_reservation_hotel` (`hotel_id`);

--
-- Indexes for table `user`
--
ALTER TABLE `user`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `id_user` (`id_user`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `adoption_request`
--
ALTER TABLE `adoption_request`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `animal`
--
ALTER TABLE `animal`
  MODIFY `idAnimal` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `client_gestion`
--
ALTER TABLE `client_gestion`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `comment`
--
ALTER TABLE `comment`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `compte`
--
ALTER TABLE `compte`
  MODIFY `id_compte` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `disponibilite`
--
ALTER TABLE `disponibilite`
  MODIFY `id_disponibilite` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `friendship`
--
ALTER TABLE `friendship`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `friend_request`
--
ALTER TABLE `friend_request`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `hotel`
--
ALTER TABLE `hotel`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `manager_account`
--
ALTER TABLE `manager_account`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `notification`
--
ALTER TABLE `notification`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `orders`
--
ALTER TABLE `orders`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `panier`
--
ALTER TABLE `panier`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `payments`
--
ALTER TABLE `payments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `post`
--
ALTER TABLE `post`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `post_reaction`
--
ALTER TABLE `post_reaction`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT for table `post_report`
--
ALTER TABLE `post_report`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `post_share`
--
ALTER TABLE `post_share`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `produit`
--
ALTER TABLE `produit`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `promo_codes`
--
ALTER TABLE `promo_codes`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `reclamation`
--
ALTER TABLE `reclamation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `rendezvous`
--
ALTER TABLE `rendezvous`
  MODIFY `id_rdv` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `reponse`
--
ALTER TABLE `reponse`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `reservation`
--
ALTER TABLE `reservation`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `user`
--
ALTER TABLE `user`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `adoption_request`
--
ALTER TABLE `adoption_request`
  ADD CONSTRAINT `adoption_request_ibfk_1` FOREIGN KEY (`animal_id`) REFERENCES `animal` (`idAnimal`),
  ADD CONSTRAINT `fk_adoption_client` FOREIGN KEY (`client_id`) REFERENCES `user` (`id`);

--
-- Constraints for table `comment`
--
ALTER TABLE `comment`
  ADD CONSTRAINT `comment_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `comment_ibfk_2` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `comment_ibfk_3` FOREIGN KEY (`parent_comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `compte`
--
ALTER TABLE `compte`
  ADD CONSTRAINT `compte_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`);

--
-- Constraints for table `disponibilite`
--
ALTER TABLE `disponibilite`
  ADD CONSTRAINT `disponibilite_ibfk_1` FOREIGN KEY (`vet_id`) REFERENCES `user` (`id`);

--
-- Constraints for table `hotel`
--
ALTER TABLE `hotel`
  ADD CONSTRAINT `fk_hotel_manager` FOREIGN KEY (`manager_id`) REFERENCES `user` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `panier`
--
ALTER TABLE `panier`
  ADD CONSTRAINT `fk_panier_client` FOREIGN KEY (`client_id`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `panier_ibfk_1` FOREIGN KEY (`idProduit`) REFERENCES `produit` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `post`
--
ALTER TABLE `post`
  ADD CONSTRAINT `post_ibfk_1` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`);

--
-- Constraints for table `reclamation`
--
ALTER TABLE `reclamation`
  ADD CONSTRAINT `fk_reclamation_client` FOREIGN KEY (`client_id`) REFERENCES `user` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `rendezvous`
--
ALTER TABLE `rendezvous`
  ADD CONSTRAINT `rendezvous_ibfk_1` FOREIGN KEY (`client_id`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `rendezvous_ibfk_2` FOREIGN KEY (`vet_id`) REFERENCES `user` (`id`),
  ADD CONSTRAINT `rendezvous_ibfk_3` FOREIGN KEY (`animal_id`) REFERENCES `animal` (`idAnimal`),
  ADD CONSTRAINT `rendezvous_ibfk_4` FOREIGN KEY (`disponibilite_id`) REFERENCES `disponibilite` (`id_disponibilite`);

--
-- Constraints for table `reponse`
--
ALTER TABLE `reponse`
  ADD CONSTRAINT `fk_reponse_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reponse_reclamation` FOREIGN KEY (`reclamation_id`) REFERENCES `reclamation` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `reservation`
--
ALTER TABLE `reservation`
  ADD CONSTRAINT `fk_reservation_animal` FOREIGN KEY (`animal_id`) REFERENCES `animal` (`idAnimal`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reservation_client` FOREIGN KEY (`client_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_reservation_hotel` FOREIGN KEY (`hotel_id`) REFERENCES `hotel` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
