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

onInit = function(n, ...)
	-- Ecran
    screen_width = 1024
    screen_height = 768

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
	
	ball_angle = math.pi -- angle en radians (/!\ : doit toujours etre entre 0 et 2*pi)
	angle_limit = 0.3 -- limite verticale pour l'angle (1 : pas de limite / 0.1 : très limité)
	coeff_v = 12 -- coef vitesse de la balle

	
	-- Raquette 1
	paddle_1_width = 30
	paddle_1_x = 0
	if taille_raquette == 1 then
		paddle_1_height = 128
		paddle_1_y = (screen_height / 2) - (paddle_1_height / 2)
		tactos_AddObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height, "none", "Raquette.png")
	elseif taille_raquette == 2 then
		paddle_1_height = 192
		paddle_1_y = (screen_height / 2) - (paddle_1_height / 2)
		tactos_AddObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height, "none", "Raquette1_2.png")
	else
		paddle_1_height = 256
		paddle_1_y = (screen_height / 2) - (paddle_1_height / 2)
		tactos_AddObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height, "none", "Raquette1_3.png")
	end
	
	-- Mur
    mur_1_width = 30
    mur_1_height = screen_height
    mur_1_x = screen_width - mur_1_width
    mur_1_y = 0
	tactos_AddObject("IMAGE", 3, mur_1_x, mur_1_y, mur_1_width, mur_1_height, "none", "Raquette2.png")		

	-- Score
	tactos_AddObject("TEXT", 10, 0, 50, screen_width, 250, "none", "CONFIG:50,CT:black,T:navy:CenterTop")
	tactos_ModifyObject("TEXT",10, tostring(score_j1))
	
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
		tactos_AddObject("SOUND", 100, -1, -1, 1, 1, "none", "filename:raquette.wav")
		tactos_AddObject("SOUND", 101, -1, -1, 1, 1, "none", "filename:mur.wav")
		tactos_AddObject("SOUND", 102, -1, -1, 1, 1, "none", "screenreader:0")
	end

	
	-- lancer balle
	lance_balle = false
	
	tactos_Redraw()
	
	tactos_SetTimerValue(20)
	
	return 20 	-- Timer
end

onTimer = function ()
	update_state()
	draw()
end

onKey = function (cmd, ch)
	if cmd == 0x0102 then
		if ch == 32 then	-- SPACE (lance la balle)
			lance_balle = true
			
			-- Remise à 0 du score
			score_j1 = 0
			tactos_ModifyObject("TEXT",10, tostring(score_j1))
			tactos_ModifyObject("TEXT",11, "")
		end
	end
end
 

function update_state()
-- update de la position de la raquette
	x,y = tactos_GetTactosPos()
	paddle_1_y = screen_height - y - (paddle_1_height / 2)
	if paddle_1_y <= 0 then -- haut de l'écran
		paddle_1_y = 0
	elseif (paddle_1_y + paddle_1_height) >= screen_height then -- bas de l'écran
		paddle_1_y = screen_height - paddle_1_height
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
	--rebond haut
	if ball_y <= 0 then
		ball_angle = (-1 * ball_angle) % (2 * math.pi)
		ball_y = 0
	end
	
	--rebond bas
	if (ball_y + ball_height) >= screen_height then
		ball_angle = (-1 * ball_angle) % (2 * math.pi)
		ball_y = screen_height - ball_height
	end
	
	-- rebond sur le mur
	if (ball_x + ball_width) >= (screen_width - mur_1_width) then
		ball_angle = (math.pi - ball_angle) % (2 * math.pi)
		ball_x = screen_width - mur_1_width - ball_width
		
		if play_sound == true then
			tactos_PlaySound(101)
		end
		
	end
	
	--rebond sur la raquette
	if type_rebond == 1 then
		rebond_raquette_1()
	else
		rebond_raquette_2()
	end
	
-- Sortie de la balle 
    if ball_x < 0 then
		lance_balle = false
		init_ball_pos()
    end
end

function update_picots_width()
	local ligne = math.ceil(ball_x / (screen_width / 4)) -- la ligne à modifier (=profondeur de la balle)
	en_face = false
	for i = 1, 4 do
		for j = 1, 4 do
			haut_case = paddle_1_y + (j-1)*paddle_1_height/4
			bas_case = paddle_1_y + j*paddle_1_height/4
			old_picot = m_picots[i][j]
			if (5-i) == ligne and ball_y+ball_height > haut_case and ball_y < bas_case then
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

function update_picots_no_width()
	local ligne = math.ceil(ball_x / (screen_width / 4)) -- la ligne à modifier (=profondeur de la balle)
	local r = math.ceil((ball_y - paddle_1_y + ball_height) / ((ball_height + paddle_1_height) / 4)) -- colonne à modifier (=position balle par rapport à la raquette)
	if r < 0 or r > 4 then r = 0 end
	en_face = (r ~= 0)
	for i = 1, 4 do
		for j = 1, 4 do
			old_picot = m_picots[i][j]
			if (5-i) == ligne and j == r then
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

-- pas de prise en compte de l'angle d'arrivée
function rebond_raquette_1()
	if ball_x <= (paddle_1_x + paddle_1_width) and en_face == true then
		Y = ball_y - paddle_1_y + ball_height
		H = paddle_1_height + ball_height
		Z = angle_limit*math.pi/2
		
		ball_angle = (((Y/H) * (-angle_limit*math.pi)) + Z) % (2 * math.pi)
		ball_x = (paddle_1_x + paddle_1_width + 1)
		score_j1 = score_j1 + 1
		
		if play_sound == true then
			tactos_PlaySound(100)
		end
		
	end	
end

-- prise en compte de l'angle d'arrivée
function rebond_raquette_2()
	if ball_x <= (paddle_1_x + paddle_1_width) and en_face == true then
		Y = ball_y - paddle_1_y + ball_height
		H = paddle_1_height + ball_height
		Z = ((2 + angle_limit)*math.pi - 2*ball_angle) / 4
		
		ball_angle = (((Y/H) * (-angle_limit*math.pi)/2) + Z) % (2 * math.pi)
		ball_x = (paddle_1_x + paddle_1_width + 1)
		score_j1 = score_j1 + 1

		if play_sound == true then
			tactos_PlaySound(100)
		end
		
	end	
end

function init_ball_pos()
	ball_x = (screen_width / 2) - (ball_width / 2)
    ball_y = (screen_height / 2) - (ball_height / 2)
	ball_angle = math.pi
end
 
function getStimString()
	local str = ""
	for i = 1, 4 do
		for j = 1, 4 do
			str = str .. m_picots[i][j]
		end
	end
	tactos_Debug(str)
	return str
end

function draw()
-- affichage score
	tactos_ModifyObject("TEXT",10, tostring(score_j1))
	
-- affichage score final
	if lance_balle == false and score_j1 > 0 then
		tactos_ModifyObject("TEXT",11, affiche_score .. tostring(score_j1))
		
		if play_sound == true then
			tactos_ModifyObject("SOUND", 102, affiche_score .. tostring(score_j1))
			tactos_PlaySound(102)
		end
	end
	
-- affichage raquette
	tactos_SetPosObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height)
	
-- affichage balle	
	tactos_SetPosObject("IMAGE", 2, ball_x, ball_y, ball_width, ball_height)
	
-- picots
	if (changeStim == true) then
		tactos_SetStim(3, getStimString())
		changeStim = false
	end
end

