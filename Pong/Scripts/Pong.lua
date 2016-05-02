
onInit = function(n, ...)
	-- Ecran
    screen_width = 1024
    screen_height = 768

	-- Creation des 4 sections pour les picots... (pas utilisés )
	tactos_AddObject("AREA", 20, 0, 123, screen_width/4, screen_height, "none", "")
	tactos_AddObject("AREA", 21, screen_width/4, 0, screen_width/4, screen_height, "none", "")
	tactos_AddObject("AREA", 22, screen_width/2, 0, screen_width/4, screen_height, "none", "")
	tactos_AddObject("AREA", 23, screen_width*(3/4), 0, screen_width/4, screen_height, "none", "")
	
	-- ball
    ball_width = 32
    ball_height = 32
    ball_x = (screen_width / 2) - (ball_width / 2)
    ball_y = (screen_height / 2) - (ball_height / 2)
    ball_speed_x = -(screen_width/6)
    ball_speed_y = (screen_width/6)
	
	coeff_v = 0.1 -- coeff vitesse par défaut (ne pas dépasser 0.1)
	
	-- Raquette 1
    paddle_1_width = 30
    paddle_1_height = 134
    paddle_1_x = 0
    paddle_1_y = (screen_height / 2) - (paddle_1_height / 2)
    paddle_1_speed = ball_speed_y * 2

	-- Mur
    mur_1_width = 30
    mur_1_height = screen_height
    mur_1_x = screen_width - mur_1_width
    mur_1_y = 0
	
	-- Raquette 1
	tactos_AddObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height, "none", "Raquette.png")
	-- Balle 
	tactos_AddObject("IMAGE", 2, ball_x, ball_y, ball_width, ball_height, "none", "Balle.bmp")
	-- Mur
	tactos_AddObject("IMAGE", 3, mur_1_x, mur_1_y, mur_1_width, mur_1_height, "none", "Raquette2.png")	
	-- Score
	tactos_AddObject("TEXT", 10, 0, 50, screen_width, 250, "none", "CONFIG:50,CT:black,T:navy:CenterTop")
	tactos_ModifyObject("TEXT",10, tostring(score_j1))
	-- Message a la fin de la game
	tactos_AddObject("TEXT", 11, 0, screen_height*0.8, screen_width, 25, "none", "CONFIG:20,CT:black,T:navy: ")
 
	-- Remarque sur les images, la taille des images affichés depend des images elles meme... nul
	
	-- Position pour les picots...
	p0 = 0
	p1 = screen_width/4
	p2 = screen_width/2
	p3 = screen_width*(3/4)
	p4 = screen_width
	
	r0 = paddle_1_y
	r1 = paddle_1_y + paddle_1_height/4
	r2 = paddle_1_y + paddle_1_height/2
	r3 = paddle_1_y + paddle_1_height*(3/4)
	r4 = paddle_1_y + paddle_1_height
	
	p = 0
	r = 0
	
	-- score
	score_j1 = 0
	affiche_score = "Vous avez fait : "
	
	-- lancer balle
	lance_balle = false
	
	tactos_Redraw()
	
	return 500 	-- Timer
end

onTimer = function ()

-- Déplacement du joueur (avec la souris)

	x,y=tactos_GetTactosPos()
	paddle_1_y = y - (paddle_1_height/2)
	
	-- Le J1 ne peut pas sortir de l'ecran
	if paddle_1_y <= 0 then -- haut de l'écran
		paddle_1_y = 0
	elseif (paddle_1_y + paddle_1_height) >= screen_height then -- bas de l'écran
		paddle_1_y = screen_height - paddle_1_height
	end
	
	tactos_SetPosObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height)
	
-- Mouvements de la balle
	
	-- La balle rebondit "en haut" et "en bas"
	if ball_y <= 0 then
		ball_speed_y = math.abs(ball_speed_y)
	elseif (ball_y + ball_height) >= screen_height  then
		ball_speed_y = -math.abs(ball_speed_y)
	end

	-- rebondit sur le J1
	if ball_x <= paddle_1_width and
		(ball_y + ball_height) >= paddle_1_y and
		ball_y < (paddle_1_y + paddle_1_height)
	then
		ball_speed_x = math.abs(ball_speed_x)
		score_j1 = score_j1 + 1
		tactos_ModifyObject("TEXT",10, tostring(score_j1))
	end

	-- rebondit sur le mur
	if (ball_x + ball_width) >= (screen_width - mur_1_width) and
		(ball_y + ball_height) >= mur_1_y and
		ball_y < (mur_1_y + mur_1_height)
	then
		ball_speed_x = -math.abs(ball_speed_x)
	end

	-- Sortie de balle 
    if ball_x + ball_width < 0 or ball_x > screen_width then
		lance_balle = false
		
		-- Replacement de la balle au centre
		ball_x = (screen_width / 2) - (ball_width / 2)
		ball_y = (screen_height / 2) - (ball_height / 2)
		ball_speed_x = -(screen_width/6)
		ball_speed_y = (screen_width/6)
		
		-- Affichage du score établie
		tactos_ModifyObject("TEXT",11, affiche_score .. tostring(score_j1))
		
    end

	if lance_balle == true then
		-- Déplacement de la balle
		ball_x = ball_x + (ball_speed_x * coeff_v)
		ball_y = ball_y + (ball_speed_y * coeff_v)
	end
	
	tactos_SetPosObject("IMAGE", 2, ball_x, ball_y, ball_width, ball_height)
	--tactos_Redraw()

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



-- LA PARTIE EN DESSOUS N EST PAS ENCORE TESTEE !!!



-- TRouve la position de la balle par rapport au terrain et à la raquette
function pos_balle_devant_r(raquette)
	-- Si joueur 1
	if raquette == 1 then
		p = 0
		r = 0
		-- si la balle se situe devant la raquette
		if (ball_y + ball_height) >= paddle_1_y and
			ball_y < (paddle_1_y + paddle_1_height) then
			
			-- position de la balle par rapport au terrain(4sections)
			if (ball_x + ball_width) >= p0 and ball_x <= p1 then
				p = 1
			elseif (ball_x + ball_width) >= p1 and ball_x <= p2 then
				p = 2
			elseif (ball_x + ball_width) >= p2 and ball_x <= p3 then
				p = 3
			elseif (ball_x + ball_width) >= p3 and ball_x <= p4 then
				p = 4
			end
			
			-- position de la balle par rapport à la raquette :
			r0 = paddle_1_y
			r1 = paddle_1_y + paddle_1_height/4
			r2 = paddle_1_y + paddle_1_height/2
			r3 = paddle_1_y + paddle_1_height*(3/4)
			r4 = paddle_1_y + paddle_1_height
			
			if (ball_y + ball_height) >= r0 and ball_y <= r1 then
				r = 1
			elseif (ball_y + ball_height) >= r1 and ball_y <= r2 then
				r = 2
			elseif (ball_y + ball_height) >= r2 and ball_y <= r3 then
				r = 3
			elseif (ball_y + ball_height) >= r3 and ball_y <= r4 then
				r = 4
			end
			
		else
			-- position inutile (aucun picot ne doit etre levé)
			p = 0
			r = 0
		end
	end
end


function Mise_a_jour_picots(x,y)

end








-- TEST NON CONCLUANT ...


--[[
onKey = function (cmd, ch)

	-- Appuie sur une touche du clavier
	if cmd == 0x0102 then
		if ch == 122 then	-- Z (haut)
			paddle_1_y = paddle_1_y - (paddle_1_speed * 0.1)
		elseif ch == 115 then	-- S (bas)
			paddle_1_y = paddle_1_y + (paddle_1_speed * 0.1)
		elseif ch == 112 then -- p (pause?)

		end
		
		-- Le J1 ne peut pas sortir de l'ecran
		if paddle_1_y <= 0 then -- haut de l'écran
			paddle_1_y = 0
		elseif (paddle_1_y + paddle_1_height) >= screen_height then -- bas de l'écran
			paddle_1_y = screen_height - paddle_1_height
		end
		
		-- Mise à jour de la raquette 1
		tactos_SetPosObject("IMAGE", 1, paddle_1_x, paddle_1_y, paddle_1_width, paddle_1_height)
		--tactos_Redraw(0, screen_height, paddle_1_y, screen_height)		
	end
end
]]