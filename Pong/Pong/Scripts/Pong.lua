-- options globales

-- jouer les sons
play_sound = true

-- utiliser la largeur réelle de la balle pour lever les picots (si false, pas plus d'1 seul picot levé en même temps)
-- si ce paramètre vaut true, le nombre de picots levés simultanéments dépend de la largeur de la balle
use_ball_width = true

-- type de rebonds sur raquette (1 = pas prise en compte angle d'arrivée / 2 = prise en compte angle d'arrivée)
type_rebond = 1

-- Taille de la balle (1 2 ou 3 du plus petit au plus grand)	-- Normal : 1
taille_balle = 1

-- Taille de la raquette (1 2 ou 3 du plus petit au plus grand)	-- Normal : 1
taille_raquette = 1


--****** Initialisation ******
onInit = function(n, ...)
	-- Ecran
    screen_width = 800
    screen_height = 900
	display = "on"
	
	-- ball
	if taille_balle == 1 then
		ball_width = 32
		ball_height = 32
		ball_x = (screen_width / 2) - (ball_width / 2)
		ball_y = (screen_height / 2) - (ball_height / 2)
		tactos_AddObject("IMAGE", 2, ball_x, ball_y, ball_width, ball_height, "none", "Balle.bmp")
	elseif taille_balle == 2 then
		ball_width = 48
		ball_height = 48
		ball_x = (screen_width / 2) - (ball_width / 2)
		ball_y = (screen_height / 2) - (ball_height / 2)
		tactos_AddObject("IMAGE", 2, ball_x, ball_y, ball_width, ball_height, "none", "Balle2.bmp")
	else
		ball_width = 64
		ball_height = 64
		ball_x = (screen_width / 2) - (ball_width / 2)
		ball_y = (screen_height / 2) - (ball_height / 2)
		tactos_AddObject("IMAGE", 2, ball_x, ball_y, ball_width, ball_height, "none", "Balle3.bmp")
	end
  	ball_angle = (3*math.pi)/2 -- angle en radians (/!\ : doit toujours etre entre 0 et 2*pi)
	angle_limit = 0.3 -- limite verticale pour l'angle (1 : pas de limite / 0.1 : très limité)
	coeff_v = 8 -- coef vitesse de la balle


	-- Raquette 1
	paddle_1_height = 30
	paddle_1_x = 0
	paddle_1_y = screen_height - paddle_1_height
	if taille_raquette == 1 then
		paddle_1_width = 128
		tactos_AddObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height, "none", "Raquette.png")
	elseif taille_raquette == 2 then
		paddle_1_width = 192
		tactos_AddObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height, "none", "Raquette1_2.png")
	else
		paddle_1_width = 256
		tactos_AddObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height, "none", "Raquette1_3.png")
	end

	-- Mur
    mur_1_height = 30
    mur_1_width = screen_width
    mur_1_x = 0
    mur_1_y = 0
	tactos_AddObject("IMAGE", 3, mur_1_x, mur_1_y, mur_1_width, mur_1_height, "none", "mur.png")
	
	-- Score
	tactos_AddObject("TEXT", 10, 0, 50, screen_width, 250, "none", "CONFIG:50,CT:black,T:navy:CenterTop")
	tactos_ModifyObject("TEXT",10, tostring(score_j1))

	name_file = "../data/Profil/_Cookies/_Pong_High_Score_.txt"
	file = io.open(name_file, "r")
	if file~=nil then
		high_score = file:read("*number")
		file:close()
	else
		high_score = 0
	end

	-- Meilleur score
	tactos_AddObject("TEXT", 12, 0, 50, screen_width, 250, "none", "CONFIG:20:white:red:")
	tactos_ModifyObject("TEXT", 12, "Meilleur score : " .. high_score);

	-- Message a la fin de la game
	tactos_AddObject("TEXT", 11, 0, screen_height*0.8, screen_width, 25, "none", "CONFIG:20,CT:black,T:navy: ")

	-- Coordonées du picot à lever
	m_picots = {} -- création de la matrice de picots
	for i = 1, 4 do
		m_picots[i] = {} -- création d'un nouveau rang
		for j = 1, 4 do
			m_picots[i][j] = 0 -- initialisation
		end
	end
	changeStim = true
	en_face = false

	-- score
	score_j1 = 0
	affiche_score = "Vous avez fait : "

	-- Sons
	if play_sound == true then
		tactos_AddObject("SOUND", 200, -1, -1, 1, 1, "none", "filename:raquette.wav")
		tactos_AddObject("SOUND", 201, -1, -1, 1, 1, "none", "filename:mur.wav")
		tactos_AddObject("SOUND", 202, -1, -1, 1, 1, "none", "screenreader:0")
	end


	-- lancer balle
	lance_balle = false

	tactos_Redraw()

	tactos_SetTimerValue(20)

	return 20 	-- Timer
end


--****** Moteur ******
onTimer = function ()
	update_state()
	draw()
end

function update_state()
-- update de la position de la raquette
	x,y = tactos_GetTactosPos()
	paddle_1_x = x - (paddle_1_width / 2)
	if paddle_1_x <= 0 then -- gauche de l'écran
		paddle_1_x = 0
	elseif (paddle_1_x + paddle_1_width) >= screen_width then -- droite de l'écran
		paddle_1_x = screen_width - paddle_1_width
	end

-- update de la position de la balle
	if lance_balle == true then
		ball_x = ball_x + (math.cos(ball_angle) * coeff_v)
		ball_y = ball_y - (math.sin(ball_angle) * coeff_v)
	end

-- update du tableau des picots
	if use_ball_width == true then
		update_picots_width()
	else
		update_picots_no_width()
	end

-- rebonds de la balle
	--rebond gauche
	if ball_x <= 0 then
		ball_angle = (math.pi - ball_angle) % (2 * math.pi)
		ball_x = 0
	end

	--rebond droite
	if (ball_x + ball_width) >= screen_width then
		ball_angle = (math.pi - ball_angle) % (2 * math.pi)
		ball_x = screen_width - ball_width
	end

	-- rebond sur le mur (haut)
	if ball_y <= mur_1_height then
		ball_angle = (-1 * ball_angle) % (2 * math.pi)
		ball_y = mur_1_height

		--on joue le son du rebond
		if play_sound == true then
			tactos_PlaySound(201)
		end
	end

	--rebond sur la raquette
	if type_rebond == 1 then
		rebond_raquette_1()
	else
		rebond_raquette_2()
	end

-- Sortie de la balle
    if (ball_y + ball_height) > screen_height then
		gameover()
    end
end

function draw()
--affichage graphique
	-- affichage score
	tactos_ModifyObject("TEXT",10, tostring(score_j1))

	-- affichage score final
	if lance_balle == false and score_j1 > 0 then
		tactos_ModifyObject("TEXT",11, affiche_score .. tostring(score_j1))
		tactos_ModifyObject("TEXT", 12, "Meilleur score : " .. high_score);
	end

	-- affichage raquette
	tactos_SetPosObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height)

	-- affichage balle
	if display == "on" then
		tactos_SetPosObject("IMAGE", 2, ball_x, ball_y, ball_width, ball_height)
	elseif display == "switch-off" then
		tactos_SetPosObject("IMAGE", 2, screen_width, 0, ball_width, ball_height)
		display = "off"
	end
	
-- déclenchement des picots
	if (changeStim == true) then
		tactos_SetStim(3, getStimString())
		changeStim = false
	end
end


--****** Appuis clavier ******
onKey = function (cmd, ch)
	if cmd == 0x0102 then
		if ch == 32 and lance_balle == false then -- SPACE (lance la balle)
			lance_balle = true
			-- Remise à 0 du score
			score_j1 = 0
			coeff_v = 8
			tactos_ModifyObject("TEXT",10, tostring(score_j1))
			tactos_ModifyObject("TEXT",11, "")
		elseif ch == 100 then -- D (display)
			if display == "on" then
				display = "switch-off"
			else
				display = "on"
			end
		end
	end
end


--****** Fonctions de service ******
-- initialisation de la position de la balle
function init_ball_pos()
	ball_x = (screen_width / 2) - (ball_width / 2)
    ball_y = (screen_height / 2) - (ball_height / 2)
	ball_angle = (3 * math.pi) / 2
end

-- récupération d'une chaine de caractères correspondant aux picots à lever (du type "0000000100000000")
function getStimString()
	local str = ""
	for i = 1, 4 do
		for j = 1, 4 do
			str = str .. m_picots[i][j]
		end
	end
	return str
end

function file_exists(name)
	local f=io.open(name,"r")
	if f~=nil then
		io.close(f)
		return true
	else
		return false
	end
end

function gameover()
	lance_balle = false
	init_ball_pos()
	
	-- annonce du score final
	if play_sound == true then
		tactos_ModifyObject("SOUND", 202, "screenreader:" .. affiche_score .. " " .. tostring(score_j1))
		tactos_PlaySound(202)
	end
	
	-- MAJ du highscore
	if(score_j1 > high_score) then
		high_score = score_j1
		file = io.open(name_file, "w+")
		file:write(tostring(high_score), " ")
		file:write("\n")
		file:close()
	end
end


--****** Fonctions de calcul de l'état des picots ******
-- plusieurs picots peuvent être levés simultanément (prise en compte de la largeur réelle de la balle)
function update_picots_width()
	local ligne = math.ceil(ball_y / (screen_height / 4)) -- la ligne à modifier (=profondeur de la balle)
	en_face = false
	for i = 1, 4 do
		for j = 1, 4 do
			gauche_case = paddle_1_x + (j-1)*paddle_1_width/4
			droite_case = paddle_1_x + j*paddle_1_width/4
			old_picot = m_picots[i][j]
			if i == ligne and ball_x+ball_width > gauche_case and ball_x < droite_case then
				m_picots[i][j] = 1
				en_face = true
			else
				m_picots[i][j] = 0
			end
			if old_picot ~= m_picots[i][j] then
				changeStim = true
			end
		end
	end
end

-- pas de prise en compte de la largeur de la balle : il ne peut y avoir qu'un seul picot levé à chaque fois, quelle que soit la largeur réelle de la balle
function update_picots_no_width()
	local ligne = math.ceil(ball_y / (screen_height / 4)) -- la ligne à modifier (=profondeur de la balle)
	local r = math.ceil((ball_x - paddle_1_x + ball_width) / ((ball_width + paddle_1_width) / 4)) -- colonne à modifier (=position balle par rapport à la raquette)
	if r < 0 or r > 4 then r = 0 end
	en_face = (r ~= 0)
	for i = 1, 4 do
		for j = 1, 4 do
			old_picot = m_picots[i][j]
			if i == ligne and j == r then
				m_picots[i][j] = 1
			else
				m_picots[i][j] = 0
			end
			if old_picot ~= m_picots[i][j] then
				changeStim = true
			end
		end
	end
end

--****** Fonctions de calcul du rebond sur la raquette ******
-- pas de prise en compte de l'angle d'arrivée
function rebond_raquette_1()
	if (ball_y + ball_height) >= (screen_height - paddle_1_height) and en_face == true then -- condition de collision sur la raquette
		-- variables locales (cf trigonométrie)
		local e = angle_limit*math.pi/2
		X = ball_x - paddle_1_x + ball_width
		W = paddle_1_width + ball_width

		-- mise à jour de l'angle de la balle et du score
		ball_angle = ((X/W) * (-e*2) + math.pi/2 + e) % (2 * math.pi)
		ball_y = (screen_height - paddle_1_height - ball_height - 1) -- pour éviter les bugs de collision, on replace la balle au dessus de la raquette
		score_j1 = score_j1 + 1

		-- on joue le son du rebond
		if play_sound == true then
			tactos_PlaySound(200)
		end
		-- la vitesse de la balle augmente à chaque rebond sur la raquette
		coeff_v = coeff_v + 0.2
	end
end

-- prise en compte de l'angle d'arrivée
function rebond_raquette_2()
	if (ball_y + ball_height) >= (screen_height - paddle_1_height) and en_face == true then -- condition de collision sur la raquette
		-- variables locales (cf trigonométrie)
		local e = angle_limit*math.pi/2
		local med = (5*math.pi - 2*ball_angle) / 4
		X = ball_x - paddle_1_x + ball_width
		W = paddle_1_width + ball_width

		-- mise à jour de l'angle de la balle et du score
		ball_angle = ((X/W) * (-e) + med + (e/2)) % (2 * math.pi)
		ball_y = (screen_height - paddle_1_height - ball_height - 1) -- pour éviter les bugs de collision, on replace la balle au dessus de la raquette
		score_j1 = score_j1 + 1

		-- on joue le son du rebond
		if play_sound == true then
			tactos_PlaySound(200)
		end
		-- la vitesse de la balle augmente à chaque rebond sur la raquette
		coeff_v = coeff_v + 0.2
	end
end




