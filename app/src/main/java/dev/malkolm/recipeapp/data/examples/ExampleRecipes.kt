package dev.malkolm.recipeapp.data.examples

import dev.malkolm.recipeapp.domain.model.Attachment
import dev.malkolm.recipeapp.domain.model.RecipeDraft
import dev.malkolm.recipeapp.domain.model.Tag

/**
 * Built-in example recipes, added from Settings. Written in the app's own formats:
 * - ingredients: one per line, "Name (amount)", with "Heading:" lines grouping them (see IngredientListItem)
 * - notes: the method, one step per paragraph; cook mode splits steps on blank lines
 * - tips: a text attachment
 *
 * Ids are fixed so adding the examples twice updates the same recipes instead of duplicating them.
 */
object ExampleRecipes {
    const val TAG = "Example"

    fun all(newId: () -> String): List<RecipeDraft> = listOf(
        fluffyPancakes(newId),
        swedishPancakes(newId),
        cinnamonBuns(newId),
        bolognese(newId),
        greenCurry(newId),
        tomatoSoup(newId)
    )

    private fun tags(newId: () -> String, vararg names: String) = (listOf(TAG) + names).map { Tag.of(newId(), it) }

    private fun tips(id: String, text: String) = Attachment.Text(id = id, title = "Tips", text = text.trimIndent())

    private fun fluffyPancakes(newId: () -> String) = RecipeDraft(
        id = "example-fluffy-pancakes",
        title = "Fluffy pancakes",
        servings = 4,
        cookingTimeMinutes = 35,
        tags = tags(newId, "Breakfast", "Vegetarian"),
        ingredients =
            """
            Batter:
            Plain flour (300 g)
            Baking powder (1 tbsp)
            Sugar (2 tbsp)
            Salt (1/2 tsp)
            Eggs (2)
            Milk (4 dl)
            Butter, melted (50 g)
            Vanilla extract (1 tsp)
            For the pan and serving:
            Butter for frying (1 tbsp)
            Maple syrup
            Fresh berries (200 g)
            """.trimIndent(),
        notes =
            """
            Whisk the flour, baking powder, sugar and salt together in a large bowl. Make a well in the middle.

            Melt the butter and let it cool for a minute. In a jug, whisk the milk, eggs and vanilla together, then whisk in the melted butter.

            Pour the wet ingredients into the well and fold together with a spatula until just combined. Small lumps are fine: over-mixing makes the pancakes tough and flat. Let the batter rest for 10 minutes while the pan heats up; it will thicken and get bubbly.

            Heat a frying pan or griddle over medium heat. Add a small knob of butter and wipe most of it away with kitchen paper, so there is only a thin film.

            Pour about 1/2 dl of batter per pancake into the pan. Cook for 2 to 3 minutes, until bubbles appear on the surface and the edges look set and dry.

            Flip and cook for another 1 to 2 minutes until golden and cooked through. Lower the heat if they brown before the middle is done.

            Keep the finished pancakes warm on a rack in a 90 °C oven while you cook the rest. Serve stacked with maple syrup and fresh berries.
            """.trimIndent(),
        attachments =
            listOf(
                tips(
                    "example-fluffy-pancakes-tips",
                    """
                    The first pancake is a test: use it to adjust the heat.
                    For even fluffier pancakes, swap the milk for buttermilk and add 1/2 tsp bicarbonate of soda.
                    Leftovers freeze well. Reheat them in a toaster.
                    """
                )
            )
    )

    private fun swedishPancakes(newId: () -> String) = RecipeDraft(
        id = "example-swedish-pancakes",
        title = "Swedish pancakes (pannkakor)",
        servings = 4,
        cookingTimeMinutes = 40,
        tags = tags(newId, "Swedish", "Vegetarian"),
        ingredients =
            """
            Batter:
            Plain flour (2 1/2 dl)
            Salt (1/2 tsp)
            Milk (6 dl)
            Eggs (3)
            Butter, melted (2 tbsp)
            For the pan and serving:
            Butter for frying
            Lingonberry or strawberry jam
            Whipped cream (2 dl cream)
            """.trimIndent(),
        notes =
            """
            Whisk the flour and salt with half of the milk into a thick, smooth batter. Starting with only some of the milk is what keeps it free of lumps.

            Whisk in the rest of the milk, then the eggs and the melted butter. Let the batter rest for 15 minutes.

            Heat a frying pan over medium-high heat and melt a little butter in it.

            Pour in about 3/4 dl of batter and tilt the pan straight away so it spreads into a thin, even layer.

            Cook for about 1 minute, until the edges are lacy and golden and the top looks dry. Flip with a spatula and cook for another 30 seconds.

            Stack the pancakes on a plate as you go. Stir the batter now and then, since the flour settles. Serve rolled up or folded, with jam and whipped cream.
            """.trimIndent(),
        attachments =
            listOf(
                tips(
                    "example-swedish-pancakes-tips",
                    """
                    Thin is the goal: if the pancakes come out thick, add a splash more milk.
                    Oven pancake (ugnspannkaka): pour the same batter into a buttered oven dish, add fried bacon pieces and bake at 225 °C for 25 to 30 minutes.
                    """
                )
            )
    )

    private fun cinnamonBuns(newId: () -> String) = RecipeDraft(
        id = "example-cinnamon-buns",
        title = "Cinnamon buns (kanelbullar)",
        servings = 30,
        cookingTimeMinutes = 150,
        tags = tags(newId, "Baking", "Swedish"),
        ingredients =
            """
            Dough:
            Butter (150 g)
            Milk (5 dl)
            Fresh yeast (50 g)
            Sugar (1 dl)
            Salt (1/2 tsp)
            Ground cardamom (2 tsp)
            Plain flour (about 13 dl)
            Filling:
            Butter, soft (150 g)
            Sugar (1 dl)
            Ground cinnamon (1 1/2 tbsp)
            Topping:
            Egg (1)
            Water (1 tbsp)
            Pearl sugar
            """.trimIndent(),
        notes =
            """
            Melt the butter in a saucepan, add the milk and warm it to finger temperature, about 37 °C. Any hotter kills the yeast.

            Crumble the yeast into a large bowl and stir in a little of the warm milk until dissolved. Add the rest of the milk mixture, the sugar, salt and cardamom.

            Add the flour a little at a time and work it into a dough. Knead for about 10 minutes, by hand or in a machine, until smooth and elastic. It should be soft and only slightly sticky: add flour sparingly.

            Cover the bowl with a tea towel and let the dough rise in a warm place for 30 to 45 minutes, until doubled in size.

            Meanwhile, stir the soft butter, sugar and cinnamon together into a smooth filling.

            Tip the dough onto a floured surface, knead briefly and divide it in two. Roll each half out into a rectangle of about 30 x 40 cm.

            Spread half of the filling evenly over each rectangle, all the way to the edges. Roll up tightly from the long side and cut each roll into 15 slices.

            Place the slices cut side up in paper cases on baking trays. Cover and let them rise for another 30 to 40 minutes, until puffy. Heat the oven to 225 °C.

            Whisk the egg with the water, brush the buns and sprinkle with pearl sugar.

            Bake in the middle of the oven for 8 to 10 minutes, until golden brown. Let them cool under a tea towel so they stay soft.
            """.trimIndent(),
        attachments =
            listOf(
                tips(
                    "example-cinnamon-buns-tips",
                    """
                    Dry yeast instead of fresh: use 2 packets (about 22 g) and mix it with the flour rather than the milk.
                    Freshly crushed cardamom seeds taste much stronger than ready-ground.
                    The buns freeze very well. Thaw at room temperature or warm them briefly in the oven.
                    """
                )
            )
    )

    private fun bolognese(newId: () -> String) = RecipeDraft(
        id = "example-bolognese",
        title = "Spaghetti bolognese",
        servings = 6,
        cookingTimeMinutes = 150,
        tags = tags(newId, "Dinner", "Italian"),
        ingredients =
            """
            Sauce:
            Olive oil (2 tbsp)
            Pancetta or bacon, diced (100 g)
            Yellow onion (1)
            Carrot (1)
            Celery stalk (1)
            Garlic cloves (2)
            Minced beef (500 g)
            Tomato paste (2 tbsp)
            Milk (1 dl)
            Red wine (1 1/2 dl)
            Crushed tomatoes (400 g can)
            Beef stock (2 dl)
            Bay leaf (1)
            Nutmeg (1 pinch)
            Salt and black pepper
            To serve:
            Spaghetti or tagliatelle (500 g)
            Parmesan, grated (50 g)
            """.trimIndent(),
        notes =
            """
            Chop the onion, carrot and celery very finely: this base (soffritto) should almost melt into the sauce. Finely chop the garlic.

            Heat the olive oil in a large, heavy pot over medium heat. Fry the pancetta for 3 minutes, then add the onion, carrot, celery and a pinch of salt. Cook gently for about 10 minutes, stirring, until soft but not browned. Add the garlic for the last minute.

            Turn up the heat, add the mince and break it up with a wooden spoon. Cook for about 8 minutes, until no longer pink and starting to brown.

            Stir in the tomato paste and cook for 1 to 2 minutes. Pour in the milk and simmer until it has almost disappeared; it makes the meat tender.

            Add the wine and let it bubble until it has mostly evaporated. Then add the crushed tomatoes, stock, bay leaf and nutmeg.

            Lower the heat so the sauce barely bubbles. Simmer with the lid slightly ajar for at least 1 1/2 hours, preferably 2 to 3, stirring now and then. Add a splash of water if it gets too thick.

            Remove the bay leaf and season well with salt and pepper.

            Cook the pasta in plenty of well-salted water, 1 minute less than the packet says. Save 1 dl of the pasta water before draining.

            Toss the pasta with the sauce and a splash of the pasta water over low heat for a minute, so the sauce clings. Serve with grated parmesan.
            """.trimIndent(),
        attachments =
            listOf(
                tips(
                    "example-bolognese-tips",
                    """
                    Tastes even better the next day, so it is worth making a double batch.
                    Use half beef and half pork mince for a richer sauce.
                    No wine? Use extra stock with 1 tsp red wine vinegar.
                    """
                )
            )
    )

    private fun greenCurry(newId: () -> String) = RecipeDraft(
        id = "example-green-curry",
        title = "Thai green curry with chicken",
        servings = 4,
        cookingTimeMinutes = 35,
        tags = tags(newId, "Dinner", "Asian"),
        ingredients =
            """
            Curry:
            Neutral oil (1 tbsp)
            Green curry paste (3-4 tbsp)
            Coconut milk (400 ml can)
            Chicken thigh fillets (500 g)
            Chicken stock (1 dl)
            Fish sauce (1 tbsp)
            Palm or brown sugar (1 tsp)
            Kaffir lime leaves (4)
            Green beans (150 g)
            Red bell pepper (1)
            Red chilli (1)
            Thai basil (1 handful)
            Lime (1/2)
            To serve:
            Jasmine rice (3 dl)
            """.trimIndent(),
        notes =
            """
            Rinse the rice and start cooking it according to the packet. Slice the chicken into bite-sized pieces, cut the beans into short lengths, slice the pepper and chilli.

            Without shaking the can, spoon the thick coconut cream off the top of the coconut milk. Keep the thin milk for later.

            Heat the oil in a wok or large pan over medium-high heat. Fry the curry paste for 1 to 2 minutes until fragrant.

            Add the thick coconut cream and fry it with the paste for 2 to 3 minutes, until the oil starts to separate. This is what gives the curry its depth.

            Add the chicken and stir for 2 minutes to coat it in the paste.

            Pour in the rest of the coconut milk and the stock, then add the lime leaves, fish sauce and sugar. Simmer for about 10 minutes, until the chicken is cooked through.

            Add the beans, pepper and chilli and simmer for 4 to 5 minutes, so the vegetables stay a little crisp.

            Take the pan off the heat and stir in the Thai basil and a squeeze of lime. Taste and balance: fish sauce for salt, sugar for sweetness, lime for sourness. Serve with the rice.
            """.trimIndent(),
        attachments =
            listOf(
                tips(
                    "example-green-curry-tips",
                    """
                    Curry pastes vary a lot in heat: start with 3 tbsp and add more at the end if you want.
                    Vegetarian: use tofu and vegetable stock, and swap the fish sauce for soy sauce.
                    Thigh fillets stay juicier than breast in a curry.
                    """
                )
            )
    )

    private fun tomatoSoup(newId: () -> String) = RecipeDraft(
        id = "example-tomato-soup",
        title = "Roasted tomato soup",
        servings = 4,
        cookingTimeMinutes = 65,
        tags = tags(newId, "Vegetarian", "Soup"),
        ingredients =
            """
            Roasting:
            Ripe tomatoes (1 kg)
            Red onion (1)
            Red bell pepper (1)
            Garlic cloves, unpeeled (4)
            Olive oil (3 tbsp)
            Sugar (1 tsp)
            Dried thyme or oregano (1 tsp)
            Salt (1 tsp)
            Black pepper
            Soup:
            Vegetable stock (5 dl)
            Cream (1 dl)
            Fresh basil (1 handful)
            To serve:
            Crusty bread
            """.trimIndent(),
        notes =
            """
            Heat the oven to 200 °C.

            Halve the tomatoes, cut the onion into wedges and the pepper into chunks. Spread everything on a baking tray, tomatoes cut side up, and tuck in the whole garlic cloves.

            Drizzle with the olive oil and sprinkle over the salt, pepper, sugar and dried herbs.

            Roast for 40 to 45 minutes, until the tomatoes have collapsed and the edges are lightly charred.

            Squeeze the soft garlic out of its skins. Tip everything from the tray, juices included, into a pot and add the stock. Simmer for 10 minutes.

            Blend until smooth with a stick blender. Stir in the cream and most of the basil.

            Taste and adjust the seasoning. Thin with a little more stock if it is too thick. Serve with the rest of the basil on top and crusty bread on the side.
            """.trimIndent(),
        attachments =
            listOf(
                tips(
                    "example-tomato-soup-tips",
                    """
                    Out-of-season tomatoes get most of their flavour back from roasting, so this works all year round.
                    Make it vegan with oat cream or a spoon of coconut milk.
                    A grilled cheese sandwich on the side makes it a full meal.
                    """
                )
            )
    )
}
