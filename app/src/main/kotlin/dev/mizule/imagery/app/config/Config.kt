/*
 * This file is part of Imagery, licensed under the MIT License.
 *
 * Copyright (c) 2023 powercas_gamer
 * Copyright (c) 2023 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.mizule.imagery.app.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class Config(
    @Comment("The port to start the application on.")
    val port: Int = 8052,

    @Comment("The base URL that this will be on, without trailing slashes.")
    val baseUrl: String = "https://i.mizule.dev",

    @Comment("The path to the file upload index.")
    val indexPath: String = "./files.json",

    @Comment("The path to the uploaded file storage directory.")
    val storagePath: String = "./storage",

    @Comment("The length of the random generated path.")
    val pathLength: Int = 8,

    @Comment(
        "Any kind of proxy services change real ip. \n" +
                "The origin ip should be available in one of the headers. \n" +
                "Nginx: X-Forwarded-For \n" +
                "Cloudflare: CF-Connecting-IP \n" +
                "Popular: X-Real-IP",
    )
    val addressHeader: String = "CF-Connecting-IP",
)
